package com.icthh.xm.uaa.util;

import com.jayway.jsonpath.JsonPath;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JsonUtils {

    public static String readField(String body, String jpath) {
        try {
            return JsonPath.read(body, jpath);
        } catch (Exception e) {
            log.debug("Error reading Xm path {}", jpath);
            return null;
        }
    }
}
