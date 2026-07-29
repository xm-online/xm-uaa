package com.icthh.xm.uaa.web.rest.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.i18n.error.domain.vm.FieldErrorVM;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;
import java.util.stream.Collectors;

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
 * <p>This class knows no constraint names. Modules that own tables contribute
 * {@link DataIntegrityErrorResolver} beans; anything no resolver claims becomes a generic
 * {@code error.data.integrity} rather than leaking the driver message.
 *
 * <p>Handlers must never rethrow: an {@code @ExceptionHandler} that throws makes Spring log the failure and
 * give up on the request entirely (see {@code ExceptionHandlerExceptionResolver#doResolveHandlerMethodException}),
 * so the common {@code Exception} fallback never runs and the client gets Spring Boot's default error page.
 *
 * <p>Ordered ahead of the common translator, but with headroom so a deployment can still put an advice
 * in front of this one.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class UaaErrorTranslator {

    private final LocalizationMessageService localizationMessageService;
    private final ObjectProvider<DataIntegrityErrorResolver> errorResolvers;

    /**
     * Translates persistence constraint failures (value too long, foreign key, unique index) into a localizable
     * {@code 400} instead of leaking {@code DataIntegrityViolationException} details.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorVM processDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);

        ErrorDefinition error = errorResolvers.orderedStream()
            .map(resolver -> resolver.resolve(ex))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElseGet(() -> new ErrorDefinition(ERROR_DATA_INTEGRITY, ERROR_DATA_INTEGRITY_MESSAGE));

        return new ErrorVM(error.getCode(), localize(error));
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
            return messageNotReadable();
        }

        MismatchedInputException cause = (MismatchedInputException) ex.getCause();
        if (cause.getPath().isEmpty()) {
            return messageNotReadable();
        }

        FieldErrorVM dto = new FieldErrorVM(ErrorConstants.ERR_VALIDATION,
            localizationMessageService.getMessage(ErrorConstants.ERR_VALIDATION));
        dto.add(resolveObjectName(cause), resolveFieldPath(cause), "typeMismatch", describeExpectedType(cause));
        return dto;
    }

    private String localize(ErrorDefinition error) {
        return localizationMessageService.getMessage(error.getCode(), null, false, error.getDefaultMessage());
    }

    private ErrorVM messageNotReadable() {
        return new ErrorVM(ErrorConstants.ERR_MESSAGE_NOT_READABLE,
            localizationMessageService.getMessage(ErrorConstants.ERR_MESSAGE_NOT_READABLE));
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
