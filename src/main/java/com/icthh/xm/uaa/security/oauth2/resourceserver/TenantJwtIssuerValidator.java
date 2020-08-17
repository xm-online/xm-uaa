package com.icthh.xm.uaa.security.oauth2.resourceserver;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//@Component
public class TenantJwtIssuerValidator implements OAuth2TokenValidator<Jwt> {

//    private final TenantRepository tenants;
    private final Map<String, JwtIssuerValidator> validators = new ConcurrentHashMap<>();

    /*public TenantJwtIssuerValidator(TenantRepository tenants) {
        this.tenants = tenants;
    }*/

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return this.validators.computeIfAbsent(toTenant(token), this::fromTenant)
            .validate(token);
    }

    private String toTenant(Jwt jwt) {
        return jwt.getIssuer().toString();
    }

    private JwtIssuerValidator fromTenant(String tenant) {
        /*return Optional.ofNullable(this.tenants.findById(tenant))
            .map(t -> t.getAttribute("issuer"))
            .map(JwtIssuerValidator::new)
            .orElseThrow(() -> new IllegalArgumentException("unknown tenant"));*/
        return null;
    }
}
