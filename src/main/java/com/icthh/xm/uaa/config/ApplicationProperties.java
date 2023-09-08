package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.lep.TenantScriptStorage;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Uaa.
 * <p>
 * Properties are configured in the application.yml file.
 * </p>
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Retry retry = new Retry();
    private final Security security = new Security();
    private final ReCaptcha reCaptcha = new ReCaptcha();
    private final Lep lep = new Lep();
    private final Communication communication = new Communication();

    private List<String> tenantIgnoredPathList;
    private List<String> timelineIgnoredHttpMethods = Collections.emptyList();
    private List<String> proxyFilterWhiteList;
    private String kafkaSystemTopic;
    private String kafkaSystemQueue;
    private boolean kafkaEnabled;
    private Integer kafkaMetadataMaxAge;
    private String tenantPropertiesPathPattern;
    private String tenantPropertiesName;
    private String tenantLoginPropertiesPathPattern;
    private String tenantLoginPropertiesName;
    private Set<String> clientGrantTypes;
    private Set<String> clientScope;
    private Set<String> defaultClientId;
    private String defaultClientSecret;
    private Integer connectTimeoutMillis;
    private Integer readTimeoutMillis;
    private String emailPathPattern;
    private boolean timelinesEnabled;
    private String dbSchemaSuffix;
    private boolean schedulerEnabled;
    private String keystoreFile;
    private String keystorePassword;
    private String privateKey;
    private String keystoreKeyName = "selfsigned";
    private boolean auditEventsEnabled;
    private boolean lastLoginDateEnabled;
    private DomainEvent domainEvent;
    private Boolean autoSystemQueueEnabled;

    @Getter
    @Setter
    private static class Retry {

        private int maxAttempts;
        private long delay;
        private int multiplier;
    }

    @Getter
    @Setter
    public static class Security {

        private Integer accessTokenValiditySeconds;
        private Integer refreshTokenValiditySeconds;
        private Integer tfaAccessTokenValiditySeconds;
        private Integer passwordEncoderStrength;
        private Boolean enablePasswordHashCaching;
        private Integer enablePasswordHashCacheSize;
    }

    @Getter
    @Setter
    public static class ReCaptcha {

        private String url;
        private String secretKey;
        private String publicKey;
        private Long registrationCaptchaPeriodSeconds;
    }

    @Getter
    @Setter
    public static class Lep {
        private TenantScriptStorage tenantScriptStorage;
        private Boolean fullRecompileOnLepUpdate;
    }

    @Getter
    @Setter
    public static class Communication {
        private boolean enabled;
    }

    @Getter
    @Setter
    public static class DomainEvent {
        private Boolean enabled;
    }
}
