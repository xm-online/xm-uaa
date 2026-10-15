package com.icthh.xm.uaa.security.misconfig;

import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.security.misconfig.MisconfigurationCheck.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Keeps nagging about unresolved misconfigurations.
 * <p>
 * Active only in the {@code prod} profile. At application startup it runs every {@link MisconfigurationCheck}
 * once; those reporting a misconfiguration are logged at {@code ERROR} and then re-run every
 * {@code application.misconfiguration-monitor.interval-seconds} seconds (default 10). A check is dropped once it
 * reports the problem is resolved; when none remain, the periodic task stops.
 */
@Slf4j
@Component
@Profile("prod")
public class MisconfigurationMonitor {

    private final List<MisconfigurationCheck> checks;
    private final long intervalSeconds;
    private volatile ScheduledExecutorService scheduler;

    public MisconfigurationMonitor(List<MisconfigurationCheck> checks,
                                   ApplicationProperties applicationProperties) {
        this.checks = checks;
        this.intervalSeconds = applicationProperties.getMisconfigurationMonitor().getIntervalSeconds();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        List<MisconfigurationCheck> failing = new CopyOnWriteArrayList<>(reportFailing(checks));
        if (failing.isEmpty()) {
            log.info("Security misconfiguration monitor: all {} check(s) passed", checks.size());
            return;
        }
        log.error("!!! SECURITY MISCONFIGURATION !!! {} unresolved issue(s); will keep reporting every {}s "
            + "until fixed", failing.size(), intervalSeconds);
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "security-misconfig");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(() -> recheck(failing), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void recheck(List<MisconfigurationCheck> failing) {
        failing.retainAll(reportFailing(failing));
        if (failing.isEmpty()) {
            log.info("Security misconfiguration monitor: all issues resolved, stopping periodic checks");
            scheduler.shutdown();
        }
    }

    private List<MisconfigurationCheck> reportFailing(List<MisconfigurationCheck> toRun) {
        List<MisconfigurationCheck> failing = new ArrayList<>();
        for (MisconfigurationCheck check : toRun) {
            Result result = check.check();
            if (result.isMisconfigured()) {
                log.error("{}", result.getMessage());
                failing.add(check);
            }
        }
        return failing;
    }
}
