package com.icthh.xm.uaa.social.twitter.api.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.social.DuplicateStatusException;
import org.springframework.social.InternalServerErrorException;
import org.springframework.social.InvalidAuthorizationException;
import org.springframework.social.MissingAuthorizationException;
import org.springframework.social.NotAuthorizedException;
import org.springframework.social.OperationNotPermittedException;
import org.springframework.social.RateLimitExceededException;
import org.springframework.social.ResourceNotFoundException;
import org.springframework.social.RevokedAuthorizationException;
import org.springframework.social.ServerDownException;
import org.springframework.social.ServerOverloadedException;
import org.springframework.social.UncategorizedApiException;
import org.springframework.social.twitter.api.InvalidMessageRecipientException;
import org.springframework.social.twitter.api.MessageTooLongException;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Subclass of {@link DefaultResponseErrorHandler} that handles errors from Twitter's
 * REST API, interpreting them into appropriate exceptions.
 */
@Slf4j
class TwitterErrorHandler extends DefaultResponseErrorHandler {

    private static final String INVALID_MESSAGE_RECIPIENT_TEXT = "You cannot send messages to users who are not following you.";
    private static final String STATUS_TOO_LONG_TEXT = "Status is over 140 characters.";
    private static final String MESSAGE_TOO_LONG_TEXT = "The text of your direct message is over 140 characters";
    private static final String DUPLICATE_STATUS_TEXT = "Status is a duplicate.";
    private static final String DAILY_RATE_LIMIT_TEXT = "User is over daily status update limit.";

    private static final int ENHANCE_YOUR_CALM = 420;
    private static final int TOO_MANY_REQUESTS = 429;

    private static final String TWITTER = "twitter";

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = response.getStatusCode();
        if (statusCode.series() == Series.SERVER_ERROR) {
            handleServerErrors(statusCode);
        } else if (statusCode.series() == Series.CLIENT_ERROR) {
            handleClientErrors(response);
        }

        // if not otherwise handled, do default handling and wrap with UncategorizedApiException
        try {
            super.handleError(response);
        } catch (Exception e) {
            throw new UncategorizedApiException(TWITTER, "Error consuming Twitter REST API", e);
        }
    }

    private static void handleClientErrors(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = response.getStatusCode();
        Map<String, Object> errorMap = extractErrorDetailsFromResponse(response);

        String errorText = "";
        if (errorMap.containsKey("error")) {
            errorText = (String) errorMap.get("error");
        } else if (errorMap.containsKey("errors")) {
            Object errors = errorMap.get("errors");
            if (errors instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> errorsList = (List<Map<String, String>>) errors;
                errorText = errorsList.get(0).get("message");
            } else if (errors instanceof String) {
                errorText = (String) errors;
            }
        }

        if (statusCode == HttpStatus.BAD_REQUEST) {
            if (errorText.contains("Rate limit exceeded.")) {
                throw new RateLimitExceededException(TWITTER);
            }
        } else if (statusCode == HttpStatus.UNAUTHORIZED) {
            if (errorText == null) {
                throw new NotAuthorizedException(TWITTER, response.getStatusText());
            } else if ("Could not authenticate you.".equals(errorText)) {
                throw new MissingAuthorizationException(TWITTER);
            } else if ("Could not authenticate with OAuth.".equals(errorText)) { // revoked token
                throw new RevokedAuthorizationException(TWITTER);
            } else if ("Invalid / expired Token".equals(errorText)) {
                // Note that Twitter doesn't actually expire tokens
                throw new InvalidAuthorizationException(TWITTER, errorText);
            } else {
                throw new NotAuthorizedException(TWITTER, errorText);
            }
        } else if (statusCode == HttpStatus.FORBIDDEN) {
            if (errorText.equals(DUPLICATE_STATUS_TEXT) || errorText.contains("You already said that")) {
                throw new DuplicateStatusException(TWITTER, errorText);
            } else if (errorText.equals(STATUS_TOO_LONG_TEXT) || errorText.contains(MESSAGE_TOO_LONG_TEXT)) {
                throw new MessageTooLongException(errorText);
            } else if (errorText.equals(INVALID_MESSAGE_RECIPIENT_TEXT)) {
                throw new InvalidMessageRecipientException(errorText);
            } else if (errorText.equals(DAILY_RATE_LIMIT_TEXT)) {
                throw new RateLimitExceededException(TWITTER);
            } else {
                throw new OperationNotPermittedException(TWITTER, errorText);
            }
        } else if (statusCode == HttpStatus.NOT_FOUND) {
            throw new ResourceNotFoundException(TWITTER, errorText);
        } else if (statusCode == HttpStatus.valueOf(ENHANCE_YOUR_CALM) || statusCode == HttpStatus
            .valueOf(TOO_MANY_REQUESTS)) {
            throw new RateLimitExceededException(TWITTER);
        }

    }

    private static void handleServerErrors(HttpStatus statusCode) throws IOException {
        if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
            throw new InternalServerErrorException(TWITTER,
                "Something is broken at Twitter. Please see http://dev.twitter.com/pages/support to report the issue.");
        } else if (statusCode == HttpStatus.BAD_GATEWAY) {
            throw new ServerDownException(TWITTER, "Twitter is down or is being upgraded.");
        } else if (statusCode == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new ServerOverloadedException(TWITTER, "Twitter is overloaded with requests. Try again later.");
        }
    }

    private static Map<String, Object> extractErrorDetailsFromResponse(ClientHttpResponse response) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        try {
            return mapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonParseException e) {
            log.error("Can not parse error details from response", e);
            return Collections.emptyMap();
        }
    }

}
