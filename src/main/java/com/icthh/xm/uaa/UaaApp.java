package com.icthh.xm.uaa;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.DefaultProfileUtil;
import io.github.jhipster.config.JHipsterConstants;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@ComponentScan("com.icthh.xm")
@EnableAutoConfiguration
@EnableConfigurationProperties({
    LiquibaseProperties.class,
    ApplicationProperties.class
})
@EnableDiscoveryClient
@Slf4j
@SpringBootApplication(scanBasePackages = {"com.icthh.xm"})
@RequiredArgsConstructor
public class UaaApp {

    private final Environment env;

    /**
     * Initializes uaa.
     * <p>
     * Spring profiles can be configured with a program argument --spring.profiles.active=your-active-profile
     * </p>
     * You can find more information on how profiles
     * work with JHipster on <a href="https://www.jhipster.tech/profiles/">https://www.jhipster.tech/profiles/</a>.
     */
    @PostConstruct
    public void initApplication() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
            && activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)) {
            log.error("You have misconfigured your application! It should not run "
                + "with both the 'dev' and 'prod' profiles at the same time.");
        }
        if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
            && activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_CLOUD)) {
            log.error("You have misconfigured your application! It should not "
                + "run with both the 'dev' and 'cloud' profiles at the same time.");
        }

        initContexts();
    }

    private static void initContexts() {
        // init tenant context, by default this is XM super tenant
        // TODO fix tenantKey upper case usage only for for Config server calls..., Postgres, H2 schema case sensitivity
        TenantKey superKey = TenantKey.valueOf(TenantKey.SUPER_TENANT_KEY_VALUE.toUpperCase());
        TenantContextUtils.setTenant(new DefaultTenantContextHolder(), superKey);

        // init logger MDC context
        MdcUtils.putRid(MdcUtils.getRid() + "::" + superKey.getValue());
    }

    @PreDestroy
    public void destroyApplication() {
        log.info("\n----------------------------------------------------------\n\t"
                + "Application {} is closing"
                + "\n----------------------------------------------------------",
            env.getProperty("spring.application.name"));
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        MdcUtils.putRid();
        // TODO fix database initialising tenant context first like in other services
        initContexts();

        SpringApplication app = new SpringApplication(UaaApp.class);
        DefaultProfileUtil.addDefaultProfile(app);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        if (StringUtils.isBlank(contextPath)) {
            contextPath = "/";
        }
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
        log.info("\n----------------------------------------------------------\n\t"
                + "Application '{}' is running! Access URLs:\n\t"
                + "Local: \t\t{}://localhost:{}{}\n\t"
                + "External: \t{}://{}:{}{}\n\t"
                + "Profile(s): \t{}\n----------------------------------------------------------",
            env.getProperty("spring.application.name"),
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            env.getActiveProfiles());

        String configServerStatus = env.getProperty("configserver.status");
        if (configServerStatus == null) {
            configServerStatus = "Not found or not setup for this application";
        }
        log.info("\n----------------------------------------------------------\n\t"
            + "Config Server: \t{}\n----------------------------------------------------------", configServerStatus);
    }
}
