package com.icthh.xm.uaa.service.user.password;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.upperCase;

@Component
public class CustomPasswordResetHandlerResolver extends AppendLepKeyResolver {

    public static final String RESET_TYPE = "resetType";

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        String resetType = getRequiredParam(method, RESET_TYPE, String.class);
        String translatedLocationTypeKey = upperCase(translateToLepConvention(resetType));
        return new String[] {
            translatedLocationTypeKey
        };
    }
}
