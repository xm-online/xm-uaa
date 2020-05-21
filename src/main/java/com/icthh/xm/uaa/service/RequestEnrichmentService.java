package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.domain.EnrichedHttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@Service
@LepService(group = "request.enrichment")
@RequiredArgsConstructor
public class RequestEnrichmentService {

    @Setter(onMethod = @__(@Autowired))
    private RequestEnrichmentService self;

    public ServletRequest enrichRequest(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            Map<String, String> customHeaders = self.getCustomHeaders(httpServletRequest);
            Map<String, String[]> customParameters = self.getCustomParameters(httpServletRequest);

            if (MapUtils.isNotEmpty(customHeaders) || MapUtils.isNotEmpty(customParameters)) {
                return new EnrichedHttpServletRequest(httpServletRequest, customHeaders, customParameters);
            }
        }

        return request;
    }

    @LogicExtensionPoint(value = "GetCustomHeaders")
    public Map<String, String> getCustomHeaders(HttpServletRequest request) {
        return Collections.emptyMap();
    }

    @LogicExtensionPoint(value = "GetCustomParameters")
    public Map<String, String[]> getCustomParameters(HttpServletRequest request) {
        return Collections.emptyMap();
    }
}
