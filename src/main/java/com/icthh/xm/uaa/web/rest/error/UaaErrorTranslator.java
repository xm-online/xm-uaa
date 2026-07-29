package com.icthh.xm.uaa.web.rest.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.i18n.error.domain.vm.FieldErrorVM;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_API_KEY_ALREADY_EXISTS;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_API_KEY_ALREADY_EXISTS_MESSAGE;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_CLIENT_IN_USE;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_CLIENT_IN_USE_MESSAGE;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_DATA_INTEGRITY;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_DATA_INTEGRITY_MESSAGE;

/**
 * UAA specific error translation, applied on top of the common
 * {@link com.icthh.xm.commons.i18n.error.web.ExceptionTranslator}.
 *
 * <p>Its purpose is to keep raw persistence and deserialization failures from reaching the client as
 * {@code 500 Internal Server Error} with SQL/Hibernate internals in the body. Every response produced here
 * follows the {@code {error, error_description, requestId}} contract, so the code can be localized per tenant
 * via {@code i18n-message.yml}.
 *
 * <p>Handlers must never rethrow: an {@code @ExceptionHandler} that throws makes Spring log the failure and
 * give up on the request entirely (see {@code ExceptionHandlerExceptionResolver#doResolveHandlerMethodException}),
 * so the common {@code Exception} fallback never runs and the client gets Spring Boot's default error page.
 * Unrecognised causes therefore fall back to a generic code instead of being propagated.
 *
 * <p>Ordered ahead of the common translator, but with headroom so a deployment can still put an advice
 * in front of this one.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class UaaErrorTranslator {

    /**
     * Constraints whose violation has a dedicated, user facing error code. The {@code api_key} table is owned by
     * the EE extension, but it lives in the same tenant schema and its foreign key points at {@code client}, so
     * the violation surfaces here. In an OSS-only deployment these constraints simply never fire.
     */
    private static final Map<String, String> CONSTRAINT_ERROR_CODES = Map.of(
        "fk_api_key_client", ERROR_CLIENT_IN_USE,
        "uk_api_key_value", ERROR_API_KEY_ALREADY_EXISTS
    );

    private static final Map<String, String> CONSTRAINT_ERROR_MESSAGES = Map.of(
        ERROR_CLIENT_IN_USE, ERROR_CLIENT_IN_USE_MESSAGE,
        ERROR_API_KEY_ALREADY_EXISTS, ERROR_API_KEY_ALREADY_EXISTS_MESSAGE
    );

    private final LocalizationMessageService localizationMessageService;

    /**
     * Translates persistence constraint failures (value too long, foreign key, unique index) into a localizable
     * {@code 400} instead of leaking {@code DataIntegrityViolationException} details.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorVM processDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);

        String code = resolveConstraintErrorCode(ex);
        String defaultMessage = CONSTRAINT_ERROR_MESSAGES.getOrDefault(code, ERROR_DATA_INTEGRITY_MESSAGE);
        return new ErrorVM(code, localizationMessageService.getMessage(code, null, false, defaultMessage));
    }

    /**
     * A body that Jackson cannot bind is a form validation problem, not a protocol problem. Reporting it as
     * {@code error.validation} with the offending field lets the client highlight the input, instead of showing
     * the opaque {@code error.messageNotReadable}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorVM processMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Message not readable", ex);

        if (!(ex.getCause() instanceof MismatchedInputException)) {
            return new ErrorVM(ErrorConstants.ERR_MESSAGE_NOT_READABLE,
                localizationMessageService.getMessage(ErrorConstants.ERR_MESSAGE_NOT_READABLE));
        }

        MismatchedInputException cause = (MismatchedInputException) ex.getCause();
        if (cause.getPath().isEmpty()) {
            return new ErrorVM(ErrorConstants.ERR_MESSAGE_NOT_READABLE,
                localizationMessageService.getMessage(ErrorConstants.ERR_MESSAGE_NOT_READABLE));
        }

        FieldErrorVM dto = new FieldErrorVM(ErrorConstants.ERR_VALIDATION,
            localizationMessageService.getMessage(ErrorConstants.ERR_VALIDATION));
        dto.add(resolveObjectName(cause), resolveFieldPath(cause), "typeMismatch", describeExpectedType(cause));
        return dto;
    }

    private String resolveConstraintErrorCode(DataIntegrityViolationException ex) {
        String details = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase();
        return CONSTRAINT_ERROR_CODES.entrySet().stream()
            .filter(entry -> details.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(ERROR_DATA_INTEGRITY);
    }

    private String resolveObjectName(MismatchedInputException cause) {
        return cause.getTargetType() == null ? "request" : cause.getTargetType().getSimpleName();
    }

    private String resolveFieldPath(MismatchedInputException cause) {
        return cause.getPath().stream()
            .map(reference -> reference.getFieldName() == null
                ? "[" + reference.getIndex() + "]"
                : reference.getFieldName())
            .collect(Collectors.joining("."));
    }

    private String describeExpectedType(MismatchedInputException cause) {
        if (cause instanceof InvalidFormatException && cause.getTargetType() != null) {
            return "Expected value of type " + cause.getTargetType().getSimpleName();
        }
        return "Invalid value";
    }
}
