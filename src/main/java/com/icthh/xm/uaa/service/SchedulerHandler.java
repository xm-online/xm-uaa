package com.icthh.xm.uaa.service;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.scheduler.service.SchedulerEventHandler;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.config.SchedulerMetricsSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerHandler implements SchedulerEventHandler {

    private final SchedulerService schedulerService;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder authContextHolder;
    private final LepManager lepManager;
    private final SchedulerMetricsSet schedulerMetricsSet;

    @Override
    public void onEvent(ScheduledEvent scheduledEvent, String tenant) {
        try {
            init(tenant);
            log.info("Receive event for tenant: {} with content: {}", tenant, scheduledEvent);
            schedulerService.onEvent(scheduledEvent);
            schedulerMetricsSet.onSuccess();
        } catch (Throwable e) {
            log.error("Error process event for tenant: "+ tenant + " with content: "+ scheduledEvent, e);
            schedulerMetricsSet.onError();
            throw e;
        } finally {
            destroy();
        }
    }

    private void init(String tenantKey) {
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);

        lepManager.beginThreadContext(threadContext -> {
            threadContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            threadContext.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    private void destroy() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

}
