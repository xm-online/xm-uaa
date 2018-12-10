package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

/**
 * Properties specific to JHipster.
 *
 * <p>Properties are configured in the application.yml file.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Retry retry = new Retry();
    private final Security security = new Security();
    private final ReCaptcha reCaptcha = new ReCaptcha();
    private final Lep lep = new Lep();

    private List<String> tenantIgnoredPathList;
    private String kafkaSystemTopic;
    private String kafkaSystemQueue;
    private boolean kafkaEnabled;
    private String tenantPropertiesPathPattern;
    private String tenantPropertiesName;
    private String tenantLoginPropertiesPathPattern;
    private String tenantLoginPropertiesName;
    private Set<String> clientGrantTypes;
    private Set<String> clientScope;
    private String tenantPath;
    private String emailPathPattern;
    private boolean timelinesEnabled;

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
    }

}
