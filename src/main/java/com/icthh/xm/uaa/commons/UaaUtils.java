package com.icthh.xm.uaa.commons;

import com.icthh.xm.uaa.config.Constants;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * The {@link UaaUtils} class.
 */
@UtilityClass
public final class UaaUtils {

    public static String getApplicationUrl(XmRequestContextHolder holder) {
        Objects.requireNonNull(holder, "holder can't be null");
        return getApplicationUrl(holder.getContext());
    }

    /**
     * Get application url from context.
     *
     * @return url
     */
    public static String getApplicationUrl(XmRequestContext requestContext) {
        Objects.requireNonNull(requestContext, "requestContext can't be null");

        String webApp = getStrValue(requestContext, Constants.REQUEST_CTX_WEB_APP);

        if (StringUtils.isBlank(webApp)) {
            String protocol = getStrValue(requestContext, Constants.REQUEST_CTX_PROTOCOL);
            String domain = getStrValue(requestContext, Constants.REQUEST_CTX_DOMAIN);
            String port = getStrValue(requestContext, Constants.REQUEST_CTX_PORT);

            String urlPort = "80".equals(port) ? "" : (":" + port);
            return protocol + "://" + domain + urlPort;
        } else {
            return webApp;
        }
    }

    private static String getStrValue(XmRequestContext requestContext, String key) {
        return requestContext.getValue(key, String.class);
    }

    public static String getRequestDomain(XmRequestContextHolder holder) {
        return getStrValue(holder.getContext(), Constants.REQUEST_CTX_DOMAIN);
    }

}
