package com.icthh.xm.uaa.validator;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserSpec;
import com.icthh.xm.uaa.service.UserSpecService;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JsonDataValidatorUnitTest {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String DATA_SPEC_TEMPLATE = "{\"type\": \"object\",\"properties\": {\"stringKey\": {\"type\": \"string\"},\"numberKey\": {\"type\": \"number\"}}}";

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private UserSpecService userSpecService;

    @InjectMocks
    private JsonDataValidator jsonDataValidator;

    @Mock
    private User user;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Test
    public void testValidationSuccessful() {
        // GIVEN
        when(user.getAuthorities()).thenReturn(List.of(ROLE_USER));
        when(userSpecService.getUserSpec(List.of(ROLE_USER))).thenReturn(List.of(new UserSpec(ROLE_USER, DATA_SPEC_TEMPLATE)));

        Map<String, Object> userData = new HashMap<>();
        userData.put("stringKey", "word");
        userData.put("numberKey", 1);
        when(user.getData()).thenReturn(userData);

        // WHEN
        boolean valid = jsonDataValidator.isValid(user, constraintValidatorContext);

        // THEN
        assertTrue("Expected validation result is true", valid);
    }

    @Test
    public void testValidationErrorForSchemeNotMatch() {
        // GIVEN
        when(user.getAuthorities()).thenReturn(List.of(ROLE_USER));
        when(userSpecService.getUserSpec(List.of(ROLE_USER))).thenReturn(List.of(new UserSpec(ROLE_USER, DATA_SPEC_TEMPLATE)));

        Map<String, Object> userData = new HashMap<>();
        userData.put("stringKey", 1);
        userData.put("numberKey", "word");
        when(user.getData()).thenReturn(userData);

        ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(constraintViolationBuilder);

        // WHEN
        boolean valid = jsonDataValidator.isValid(user, constraintValidatorContext);

        // THEN
        assertFalse("Expected validation result is false", valid);
    }

    @Test
    public void testValidationForEmptyDataButNotEmptySpec() {
        // GIVEN
        when(user.getAuthorities()).thenReturn(List.of(ROLE_USER));
        when(userSpecService.getUserSpec(List.of(ROLE_USER))).thenReturn(List.of(new UserSpec(ROLE_USER, DATA_SPEC_TEMPLATE)));

        when(user.getData()).thenReturn(Collections.emptyMap());

        // WHEN
        boolean valid = jsonDataValidator.isValid(user, constraintValidatorContext);

        // THEN
        assertFalse("Expected validation result is false", valid);
    }

    @Test
    public void testValidationForEmptySpec() {
        // GIVEN
        when(user.getAuthorities()).thenReturn(List.of(ROLE_USER));
        when(userSpecService.getUserSpec(List.of(ROLE_USER))).thenReturn(Collections.emptyList());

        // WHEN
        boolean valid = jsonDataValidator.isValid(user, constraintValidatorContext);

        // THEN
        assertTrue("Expected validation result is true", valid);
    }
}
