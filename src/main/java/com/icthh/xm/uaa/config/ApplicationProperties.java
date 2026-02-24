package com.icthh.xm.uaa.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.List;

import static com.icthh.xm.uaa.config.Constants.YEAR_IN_DAYS;

/**
 * Properties specific to Uaa.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    private CorsConfiguration cors = new CorsConfiguration();

    private Integer httpCacheTimeToLiveInDays = YEAR_IN_DAYS;

    private final Security security = new Security();

    @Getter
    @Setter
    public static class Security {

        private final OAuth2 oauth2 = new OAuth2();

        @Getter
        @Setter
        public static class OAuth2 {
            private List<String> audience = new ArrayList<>();
        }
    }
}
