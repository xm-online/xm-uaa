package com.icthh.xm.uaa.utils;

import com.icthh.xm.commons.config.domain.Configuration;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;

@UtilityClass
public class FileUtil {

    public static String readConfigFile(String path) {
        return new BufferedReader(new InputStreamReader(FileUtil.class.getResourceAsStream(path)))
            .lines().collect(Collectors.joining("\n"));
    }

    public static Map<String, Configuration> getSingleConfigMap(final String path) {
        return getSingleConfigMap(path, "");
    }

    public static Map<String, Configuration> getSingleConfigMap(String path, String content) {
        return singletonMap(path, Configuration.of().path(path).content(content).build());
    }
}
