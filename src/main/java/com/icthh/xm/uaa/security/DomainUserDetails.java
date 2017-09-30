package com.icthh.xm.uaa.security;

import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class DomainUserDetails extends User {

    private final String tenant;
    private final String userKey;
    private final Integer accessTokenValiditySeconds;
    private final Integer refreshTokenValiditySeconds;

    public DomainUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities,
        String tenant, String userKey) {
        this(username, password, authorities, tenant, userKey, null, null);
    }

    public DomainUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities,
        String tenant, String userKey, Integer accessTokenValiditySeconds, Integer refreshTokenValiditySeconds) {
        super(username, password, true, true, true, true, authorities);
        this.tenant = tenant;
        this.userKey = userKey;
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        DomainUserDetails rhs = (DomainUserDetails) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(tenant, rhs.tenant)
            .append(userKey, rhs.userKey)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(this.tenant.hashCode())
            .append(this.userKey.hashCode())
            .toHashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append("; ");
        sb.append("Tenant: ").append(this.tenant);
        sb.append("UserKey: ").append(this.userKey);
        return sb.toString();
    }
}
