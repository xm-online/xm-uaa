package com.icthh.xm.uaa.social.connect.web;

import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.domain.SocialConfig;
import com.icthh.xm.uaa.repository.SocialConfigRepository;
import com.icthh.xm.uaa.social.twitter.connect.TwitterConnectionFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.facebook.connect.FacebookConnectionFactory;
import org.springframework.social.google.connect.GoogleConnectionFactory;
import org.springframework.social.linkedin.connect.LinkedInConnectionFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Social api registry.
 */
public class DomainConnectionFactoryLocator implements ConnectionFactoryLocator {

    private final SocialConfigRepository socialConfigRepository;

    private final Map<Class<?>, String> apiTypeIndex = new HashMap<>();

    public DomainConnectionFactoryLocator(SocialConfigRepository socialConfigRepository) {
        this.socialConfigRepository = socialConfigRepository;
        initializeApis();
    }

    private void initializeApis() {
        apiTypeIndex.put(FacebookConnectionFactory.class, "facebook");
        apiTypeIndex.put(GoogleConnectionFactory.class, "google");
        apiTypeIndex.put(TwitterConnectionFactory.class, "twitter");
        apiTypeIndex.put(LinkedInConnectionFactory.class, "linkedin");
    }

    @Override
    public ConnectionFactory<?> getConnectionFactory(String providerId) {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();
        String domain = TenantContext.getCurrent().getDomain();
        Optional<SocialConfig> config = socialConfigRepository.findOneByProviderIdAndDomain(providerId, domain);
        if (config.isPresent()) {
            SocialConfig sc = config.get();
            switch (sc.getProviderId()) {
                case "facebook":
                    return new FacebookConnectionFactory(sc.getConsumerKey(), sc.getConsumerSecret());
                case "google":
                    return new GoogleConnectionFactory(sc.getConsumerKey(), sc.getConsumerSecret());
                case "twitter":
                    return new TwitterConnectionFactory(sc.getConsumerKey(), sc.getConsumerSecret());
                case "linkedin":
                    return new LinkedInConnectionFactory(sc.getConsumerKey(), sc.getConsumerSecret());
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("No provider config found for " + providerId);
    }

    @Override
    public <A> ConnectionFactory<A> getConnectionFactory(Class<A> apiType) {
        String providerId = apiTypeIndex.get(apiType);
        if (providerId == null) {
            throw new IllegalArgumentException(
                "No connection factory for API [" + apiType.getName() + "] is registered");
        }

        return (ConnectionFactory<A>) getConnectionFactory(providerId);
    }

    @Override
    public Set<String> registeredProviderIds() {
        return Collections.emptySet();
    }
}
