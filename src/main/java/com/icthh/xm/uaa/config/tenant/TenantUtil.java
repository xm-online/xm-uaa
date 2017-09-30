package com.icthh.xm.uaa.config.tenant;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

/**
 * Helper for dealing with TenantContext.
 */
@UtilityClass
public final class TenantUtil {

    /**
     * Get application url from context.
     * @return url
     */
    public static String getApplicationUrl() {
        TenantInfo info = TenantContext.getCurrent();
        if (StringUtils.isBlank(info.getWebapp())) {
            return info.getProtocol() + "://" + info.getDomain() + ("80".equals(info.getPort()) ? ""
                : (":" + info.getPort()));
        } else {
            return info.getWebapp();
        }
    }
}
