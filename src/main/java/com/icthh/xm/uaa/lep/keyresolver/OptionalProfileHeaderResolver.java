package com.icthh.xm.uaa.lep.keyresolver;

import static java.util.Optional.ofNullable;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class OptionalProfileHeaderResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return getHeaderValue("profile", String.class).map(List::of).orElse(List.of());
    }

    private <T> Optional<T> getHeaderValue(final String headerName, final Class<T> valueType) {
        return ofNullable(RequestContextHolder.getRequestAttributes())
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest)
            .map(request -> request.getHeader(headerName))
            .map(valueType::cast);
    }
}
