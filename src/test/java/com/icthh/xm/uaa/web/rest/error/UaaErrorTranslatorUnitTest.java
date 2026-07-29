package com.icthh.xm.uaa.web.rest.error;

import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_DATA_INTEGRITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UaaErrorTranslatorUnitTest {

    private static final String WIDGET_CONSTRAINT_FAILURE =
        "ERROR: duplicate key value violates unique constraint \"uk_widget_code\"";

    @Mock
    private LocalizationMessageService localizationMessageService;

    private UaaErrorTranslator translator(DataIntegrityErrorResolver... resolvers) {
        when(localizationMessageService.getMessage(anyString(), any(), anyBoolean(), anyString()))
            // stand in for tenant i18n: echo the default message back
            .thenAnswer(invocation -> invocation.getArgument(3));
        return new UaaErrorTranslator(localizationMessageService, providerOf(resolvers));
    }

    private ObjectProvider<DataIntegrityErrorResolver> providerOf(DataIntegrityErrorResolver... resolvers) {
        List<DataIntegrityErrorResolver> beans = List.of(resolvers);
        return new ObjectProvider<>() {
            @Override
            public Stream<DataIntegrityErrorResolver> orderedStream() {
                return beans.stream();
            }

            @Override
            public DataIntegrityErrorResolver getObject() {
                throw new UnsupportedOperationException();
            }

            @Override
            public DataIntegrityErrorResolver getObject(Object... args) {
                throw new UnsupportedOperationException();
            }

            @Override
            public DataIntegrityErrorResolver getIfAvailable() {
                return null;
            }

            @Override
            public DataIntegrityErrorResolver getIfUnique() {
                return null;
            }
        };
    }

    @Test
    void shouldFallBackToGenericErrorWhenNoResolverRegistered() {
        ErrorVM error = translator().processDataIntegrityViolation(
            new DataIntegrityViolationException(WIDGET_CONSTRAINT_FAILURE));

        assertEquals(ERROR_DATA_INTEGRITY, error.getError());
    }

    @Test
    void shouldUseErrorFromRegisteredResolver() {
        DataIntegrityErrorResolver widgetResolver = new ConstraintNameErrorResolver(java.util.Map.of(
            "uk_widget_code", new ErrorDefinition("error.widget.already.exists", "Widget already exists")));

        ErrorVM error = translator(widgetResolver).processDataIntegrityViolation(
            new DataIntegrityViolationException(WIDGET_CONSTRAINT_FAILURE));

        assertEquals("error.widget.already.exists", error.getError());
        assertEquals("Widget already exists", error.getError_description());
    }

    @Test
    void shouldFallBackWhenRegisteredResolverDoesNotRecogniseConstraint() {
        DataIntegrityErrorResolver otherResolver = new ConstraintNameErrorResolver(java.util.Map.of(
            "uk_something_else", new ErrorDefinition("error.other", "Other")));

        ErrorVM error = translator(otherResolver).processDataIntegrityViolation(
            new DataIntegrityViolationException(WIDGET_CONSTRAINT_FAILURE));

        assertEquals(ERROR_DATA_INTEGRITY, error.getError());
    }

    @Test
    void shouldConsultResolversInOrderAndStopAtFirstMatch() {
        DataIntegrityErrorResolver first = exception -> Optional.of(new ErrorDefinition("error.first", "First"));
        DataIntegrityErrorResolver second = exception -> Optional.of(new ErrorDefinition("error.second", "Second"));

        ErrorVM error = translator(first, second).processDataIntegrityViolation(
            new DataIntegrityViolationException(WIDGET_CONSTRAINT_FAILURE));

        assertEquals("error.first", error.getError());
    }

    @Test
    void shouldMatchConstraintNameCaseInsensitively() {
        DataIntegrityErrorResolver resolver = new ConstraintNameErrorResolver(java.util.Map.of(
            "UK_WIDGET_CODE", new ErrorDefinition("error.widget.already.exists", "Widget already exists")));

        ErrorVM error = translator(resolver).processDataIntegrityViolation(
            new DataIntegrityViolationException(WIDGET_CONSTRAINT_FAILURE));

        assertEquals("error.widget.already.exists", error.getError());
    }
}
