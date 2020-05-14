package com.icthh.xm.uaa.security.ldap;


import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageUrlParserTest {

    public static final String DYNAMIC_PARAMETER_PATTERN = "\\{(.*?)\\}";

    @Test
    public void replaceTest() {
        ImageUrlParser parser = getImageUrlParser();
        String replace = parser.replace(Map.of("userName1", "V", "userName2", "P"));
        assertEquals(replace, "http:/V/P");
    }

    @Test
    public void replaceIfWrongMappingTest() {
        ImageUrlParser parser = getImageUrlParser();
        String replace = parser.replace(Map.of("userName", "V", "userName2", "P"));
        assertEquals(replace, "http:/{userName1}/P");
    }

    @Test
    public void getParametersTest() {
        ImageUrlParser parser = getImageUrlParser();
        List<String> parameters = parser.getParameters();
        assertTrue(parameters.size() == 2);
        assertTrue(parameters.contains("userName1"));
        assertTrue(parameters.contains("userName2"));
    }

    @Test
    public void getParametersIfNothingMatchTest() {
        String imageUrl = "https://www.google.com/";
        ImageUrlParser parser = ImageUrlParser.parser(imageUrl, DYNAMIC_PARAMETER_PATTERN);
        List<String> parameters = parser.getParameters();
        assertTrue(parameters.size() == 0);
    }

    private ImageUrlParser getImageUrlParser() {
        String imageUrl = "http:/{userName1}/{userName2}";
        return ImageUrlParser.parser(imageUrl, DYNAMIC_PARAMETER_PATTERN);
    }
}
