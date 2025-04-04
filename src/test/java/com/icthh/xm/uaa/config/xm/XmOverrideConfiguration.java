package com.icthh.xm.uaa.config.xm;

import static org.mockito.Mockito.mock;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.config.client.service.TenantAliasServiceImpl;
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import com.icthh.xm.commons.lep.spring.LepUpdateMode;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import com.icthh.xm.commons.migration.db.jsonb.CustomExpression;
import com.icthh.xm.commons.migration.db.jsonb.JsonbExpression;
import com.icthh.xm.commons.migration.db.jsonb.OracleExpression;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class XmOverrideConfiguration {

    private static final String BODY = "-----BEGIN CERTIFICATE-----\n" +
        "MIIDcTCCAlmgAwIBAgIEMQoi8TANBgkqhkiG9w0BAQsFADBpMQkwBwYDVQQGEwAx\n" +
        "CTAHBgNVBAgTADEJMAcGA1UEBxMAMRkwFwYDVQQKExBjb20uaWN0aGgueG0udWFh\n" +
        "MRQwEgYDVQQLEwtEZXZlbG9wbWVudDEVMBMGA1UEAxMMSmF2YSBIaXBzdGVyMB4X\n" +
        "DTE3MDUyOTIxNTAyM1oXDTE3MDgyNzIxNTAyM1owaTEJMAcGA1UEBhMAMQkwBwYD\n" +
        "VQQIEwAxCTAHBgNVBAcTADEZMBcGA1UEChMQY29tLmljdGhoLnhtLnVhYTEUMBIG\n" +
        "A1UECxMLRGV2ZWxvcG1lbnQxFTATBgNVBAMTDEphdmEgSGlwc3RlcjCCASIwDQYJ\n" +
        "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAK9e5n2dLgj1LPiQ3Dr8sRidd9/grJq6\n" +
        "DhBSTpjjkxAA/M8BJEyiB4IQeKEn8PcH3wWcS25MHTFigwaM9cnKvqVB88D4rMR7\n" +
        "Ub6GT5JRgKj7mqmKKnZ3nk1ib7EbRhA/LsuWzRG5gJSj05fI/cBkc3yTIzgc+R8a\n" +
        "wvuWE5o3UsVSFS47wsVf0Z6TMaxZkNLZFMQG7zGMt+9M3Vx6tACJO/bASezvK4Gj\n" +
        "BAwEhpEuq/6WfYPI65GeBevMrVUW5bcbeCop7YTT9GGj6HdikY7XLiJijxdEGN8O\n" +
        "Yv5kJWE+XoFhUmx3TFkW5Vs/aEFMaIwmRGqRnMwacEjop4tC/fNvyEECAwEAAaMh\n" +
        "MB8wHQYDVR0OBBYEFNCDzzlIBnIb6whb775e51xzyi/oMA0GCSqGSIb3DQEBCwUA\n" +
        "A4IBAQAJKUO9AgRgCtAP9bqhp1ikEIHyXfm+XGKKItqHkDxQGGJTUKQGK8gZx/DK\n" +
        "d0MFC6SqEk8y0IU9WUDr8mzqmkGoPV7G0EmKzoeWYsqUkt+TM8mcuotmbSwH00K6\n" +
        "KCC2HXkv25iTQo2C8Rpr59xpQGi1Epkrjp6fnCsLzkpLHS+tHNF9OEAJ/10jeM1T\n" +
        "Ja5iOjT0YWEzDSz/R4n7nbmRWRucSTaxMk+YjMjIzLj1NlaXpgYU36MIYlQeiwGy\n" +
        "jde08SUVebqJtq5zP+5cGy3/xo0WWEZZXF7pwG8kWZ983jrZ9utA+ThUoLfunPZT\n" +
        "fmiXN/69ssMZSyCx3QBmkrKmC2Fx\n" +
        "-----END CERTIFICATE-----";

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Bean
    @Primary
    public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
        RestTemplate template = mock(RestTemplate.class);
        Mockito.when(template.exchange(Mockito.anyString(),
            Mockito.any(),
            Mockito.any(),
            Mockito.eq(String.class)))
            .thenReturn(new ResponseEntity<>(BODY, HttpStatus.OK));
        return template;
    }

    @Bean
    @Primary
    public OAuth2RestTemplate oAuth2RestTemplate() {
        OAuth2RestTemplate template = mock(OAuth2RestTemplate.class);
        Mockito.when(template.exchange(Mockito.anyString(),
            Mockito.any(),
            Mockito.any(),
            Mockito.eq(String.class)))
            .thenReturn(new ResponseEntity<>(BODY, HttpStatus.OK));
        return template;
    }

    @Bean
    public CustomExpression customExpression() {
        if (jdbcUrl.startsWith("jdbc:oracle:")) {
            log.info("Init OracleExpression");
            return new OracleExpression();
        } else {
            log.info("Init JsonbExpression");
            return new JsonbExpression();
        }
    }

}
