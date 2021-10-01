package com.icthh.xm.uaa.lep.keyresolver;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

@Component
public class LepTokenGranterKeyResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
        LepMethod method,
        LepManagerService managerService) {
        String grantType = getRequiredParam(method, "grantType", String.class);
        String translatedGrantTypeTypeKey = translateToLepConvention(grantType);
        return new String[]{
            translatedGrantTypeTypeKey
        };
    }
}
