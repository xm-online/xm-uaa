package com.icthh.xm.uaa.domain;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Getter
@Setter
public class SocialConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String providerId;
    private String consumerKey;
    private String consumerSecret;
    private String domain;


    public SocialConfig() {}

    public SocialConfig(Social social) {
        this.providerId = social.getProviderId();
        this.consumerKey = social.getConsumerKey();
        this.consumerSecret = social.getConsumerSecret();
        this.domain = social.getDomain();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SocialConfig config = (SocialConfig) o;
        return new EqualsBuilder()
            .append(domain, config.domain)
            .append(providerId, config.providerId)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(domain)
            .append(providerId)
            .hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SocialConfig{");
        sb.append("name='").append(providerId).append('\'');
        sb.append(", consumerKey='").append(consumerKey).append('\'');
        sb.append(", consumerSecret='").append(consumerSecret).append('\'');
        sb.append(", domain=").append(domain);
        sb.append('}');
        return sb.toString();
    }
}
