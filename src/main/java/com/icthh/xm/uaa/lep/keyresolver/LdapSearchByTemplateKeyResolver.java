package com.icthh.xm.uaa.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LdapSearchByTemplateKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("templateKey", String.class));
    }
}
