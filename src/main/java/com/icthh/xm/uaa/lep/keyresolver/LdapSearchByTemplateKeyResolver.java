package com.icthh.xm.uaa.lep.keyresolver;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

@Component
public class LdapSearchByTemplateKeyResolver extends AppendLepKeyResolver {
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        String templateKey = getRequiredParam(method, "templateKey", String.class);
        String translatedTemplate = translateToLepConvention(templateKey);
        return new String[] {
            translatedTemplate
        };
    }
}
