package com.icthh.xm.uaa.lep.keyresolver;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.commons.lep.SeparatorSegmentedLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@Component
public class OptionalProfileHeaderResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        return getHeaderValue("profile", String.class)
            .map(SeparatorSegmentedLepKeyResolver::translateToLepConvention)
            .map(translated -> new String[]{translated})
            .orElseGet(() -> new String[0]);
    }

    private <T> Optional<T> getHeaderValue(final String headerName, final Class<T> valueType) {
        return ofNullable(RequestContextHolder.getRequestAttributes())
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest)
            .map(request -> request.getHeader(headerName))
            .map(valueType::cast);
    }
}
