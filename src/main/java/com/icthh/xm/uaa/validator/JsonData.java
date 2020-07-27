package com.icthh.xm.uaa.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = JsonDataValidator.class)
public @interface JsonData {

    String message() default "{xm.ms.uaa.data.constraint}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
