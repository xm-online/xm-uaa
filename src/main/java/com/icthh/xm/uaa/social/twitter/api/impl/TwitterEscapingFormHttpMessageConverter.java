package com.icthh.xm.uaa.social.twitter.api.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.MultiValueMap;

/**
 * Custom FormHttpMessageConverter that meets Twitter's non-RFC1738 escaping of asterisks ('*').
 *
 * @author Craig Walls
 */
class TwitterEscapingFormHttpMessageConverter extends FormHttpMessageConverter {

    TwitterEscapingFormHttpMessageConverter() {
        setCharset(Charset.forName("UTF-8"));
        List<HttpMessageConverter<?>> partConverters = new ArrayList<>();
        partConverters.add(new ByteArrayHttpMessageConverter());
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(
            Charset.forName("UTF-8"));
        stringHttpMessageConverter.setWriteAcceptCharset(false);
        partConverters.add(stringHttpMessageConverter);
        partConverters.add(new ResourceHttpMessageConverter());
        setPartConverters(partConverters);
    }


    @Override
    public void write(MultiValueMap<String, ?> map, MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException {
        // only do special escaping if this is a non-multipart request
        if (!isMultipart(map, contentType)) {
            super.write(map, contentType, new TwitterEscapingHttpOutputMessage(outputMessage));
        } else {
            super.write(map, contentType, outputMessage);
        }
    }

    private static class TwitterEscapingHttpOutputMessage implements HttpOutputMessage {

        private HttpOutputMessage target;

        public TwitterEscapingHttpOutputMessage(HttpOutputMessage target) {
            this.target = target;
        }

        @Override
        public HttpHeaders getHeaders() {
            return target.getHeaders();
        }

        @Override
        public OutputStream getBody() throws IOException {
            return new TwitterEscapingOutputStream(target.getBody());
        }
    }

    private static class TwitterEscapingOutputStream extends OutputStream {

        private OutputStream target;

        public TwitterEscapingOutputStream(OutputStream target) {
            this.target = target;
        }

        @Override
        public void write(int b) throws IOException {
            // If more exceptions to URL encoding are found, this can be made to be mode clever than a bunch of
            // else-if blocks. But until then, this is sufficient.
            if (b == '*') {
                target.write('%');
                target.write('2');
                target.write('A');
            } else {
                target.write(b);
            }
        }
    }

    // "borrowed" from FormHttpMessageConverter
    private static boolean isMultipart(MultiValueMap<String, ?> map, MediaType contentType) {
        if (contentType != null) {
            return MediaType.MULTIPART_FORM_DATA.equals(contentType);
        }
        for (Entry<String, ?> entry : map.entrySet()) {
            if (entry.getValue() != null && !(entry.getValue() instanceof String)) {
                return true;
            }
        }
        return false;
    }

}

