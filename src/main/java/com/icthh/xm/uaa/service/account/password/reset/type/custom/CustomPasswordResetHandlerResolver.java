package com.icthh.xm.uaa.service.account.password.reset.type.custom;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.uaa.service.account.password.reset.PasswordResetHandler;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.upperCase;

@Component
public class CustomPasswordResetHandlerResolver extends AppendLepKeyResolver {

    public static final String RESET_REQUEST = "resetRequest";

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        PasswordResetHandler.PasswordResetRequest resetType = getRequiredParam(method, RESET_REQUEST, PasswordResetHandler.PasswordResetRequest.class);
        String translatedLocationTypeKey = upperCase(translateToLepConvention(resetType.getResetType()));
        return new String[] {
            translatedLocationTypeKey
        };
    }
}
