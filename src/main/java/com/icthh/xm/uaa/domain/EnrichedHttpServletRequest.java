package com.icthh.xm.uaa.domain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.enumeration;
import static java.util.Collections.singletonList;

public class EnrichedHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders;
    private final Map<String, String[]> customParameters;

    public EnrichedHttpServletRequest(HttpServletRequest request,
                                      Map<String, String> customHeaders,
                                      Map<String, String[]> customParameters) {
        super(request);
        this.customHeaders = customHeaders;
        this.customParameters = customParameters;
    }

    @Override
    public String getHeader(String name) {
        if (customHeaders.containsKey(name)) {
            return customHeaders.get(name);
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return enumeration(singletonList(customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> originalHeaderNames = Collections.list(super.getHeaderNames());
        Set<String> mergedHeaderNames = Stream.concat(originalHeaderNames.stream(), customHeaders.keySet().stream())
            .collect(Collectors.toSet());

        return enumeration(mergedHeaderNames);
    }

    @Override
    public String getParameter(String name) {
        if (customParameters.containsKey(name)) {
            return customParameters.get(name)[0];
        }
        return super.getParameter(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameterMap = new HashMap<>(super.getParameterMap());
        parameterMap.putAll(customParameters);

        return parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        ArrayList<String> superParameterNames = Collections.list(super.getParameterNames());
        Set<String> collect = Stream.concat(superParameterNames.stream(), customParameters.keySet().stream())
            .collect(Collectors.toSet());

        return enumeration(collect);
    }

    @Override
    public String[] getParameterValues(String name) {
        if (customParameters.containsKey(name)) {
            return customParameters.get(name);
        }
        return super.getParameterValues(name);
    }
}
