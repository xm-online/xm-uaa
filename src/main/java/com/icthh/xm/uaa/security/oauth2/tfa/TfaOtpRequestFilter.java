package com.icthh.xm.uaa.security.oauth2.tfa;

import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.domain.OtpChannelType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

import static com.icthh.xm.uaa.config.Constants.REQ_ATTR_TFA_OTP_CHANNEL_TYPE;
import static com.icthh.xm.uaa.config.Constants.REQ_ATTR_TFA_VERIFICATION_OTP_KEY;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

/**
 * The {@link TfaOtpRequestFilter} class.
 */
@Order
public class TfaOtpRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        HttpServletResponse responseToUse = response;
        if ("/oauth/token".equals(request.getRequestURI())) {
            responseToUse = wrapResponse(request, response);
        }

        filterChain.doFilter(request, responseToUse);
    }

    private HttpServletResponse wrapResponse(HttpServletRequest request, HttpServletResponse originalResponse) {
        return new HttpServletResponseWrapper(originalResponse) {

            @Override
            public void setStatus(int sc) {
                super.setStatus(sc);
                handleStatus(sc);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void setStatus(int sc, String sm) {
                super.setStatus(sc, sm);
                handleStatus(sc);
            }

            @Override
            public void sendError(int sc, String msg) throws IOException {
                super.sendError(sc, msg);
                handleStatus(sc);
            }

            @Override
            public void sendError(int sc) throws IOException {
                super.sendError(sc);
                handleStatus(sc);
            }

            @Override
            public HttpServletResponse getResponse() {
                return HttpServletResponse.class.cast(super.getResponse());
            }

            private void handleStatus(int statusCode) {
                HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
                if (httpStatus.is2xxSuccessful()) {
                    Object encodedOtp = RequestContextHolder.getRequestAttributes()
                        .getAttribute(REQ_ATTR_TFA_VERIFICATION_OTP_KEY, SCOPE_REQUEST);
                    if (encodedOtp != null && encodedOtp instanceof String && StringUtils.isNotBlank((String) encodedOtp)) {
                        addOtpHeaders(getResponse());
                    }
                }
            }

        };
    }

    private static void addOtpHeaders(HttpServletResponse response) {
        response.setHeader(Constants.HTTP_HEADER_TFA_OTP, "required");

        // add OTP channel type
        Object otpChannelTypeObj = RequestContextHolder.getRequestAttributes()
            .getAttribute(REQ_ATTR_TFA_OTP_CHANNEL_TYPE, SCOPE_REQUEST);
        if (otpChannelTypeObj instanceof OtpChannelType) {
            OtpChannelType otpChannelType = OtpChannelType.class.cast(otpChannelTypeObj);
            response.setHeader(Constants.HTTP_HEADER_TFA_OTP_CHANNEL, otpChannelType.getTypeName());
        }
    }

}
