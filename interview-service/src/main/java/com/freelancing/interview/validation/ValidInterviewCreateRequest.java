package com.freelancing.interview.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InterviewCreateRequestValidator.class)
@Documented
public @interface ValidInterviewCreateRequest {
    String message() default "Invalid interview create request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
