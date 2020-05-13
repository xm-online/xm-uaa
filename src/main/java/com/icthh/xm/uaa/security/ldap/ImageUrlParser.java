package com.icthh.xm.uaa.security.ldap;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageUrlParser {

    private final String imageUrl;
    private final Pattern bracePattern;

    public static ImageUrlParser parser(String imageUrl, String dynamicParameterPattern) {
        Pattern bracePattern = Pattern.compile(dynamicParameterPattern);
        return new ImageUrlParser(imageUrl, bracePattern);
    }

    public List<String> getParameters() {
        Matcher matcher = bracePattern.matcher(imageUrl);
        List<String> result = Lists.newArrayList();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    public String replace(Map<String, String> parameters) {
        String resultImageUrl = imageUrl;
        Matcher matcher = bracePattern.matcher(imageUrl);
        while (matcher.find()) {
            String paramValueWithPattern = matcher.group();
            String paramValue = matcher.group(1);
            String value = parameters.get(paramValue);
            if(Objects.nonNull(value)) {
                resultImageUrl = StringUtils.replace(resultImageUrl, paramValueWithPattern, value);
            }
        }
        return resultImageUrl;
    }
}
