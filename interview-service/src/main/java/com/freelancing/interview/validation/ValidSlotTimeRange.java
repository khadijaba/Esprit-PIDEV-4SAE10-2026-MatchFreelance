package com.freelancing.interview.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SlotTimeRangeValidator.class)
@Documented
public @interface ValidSlotTimeRange {
    String message() default "endAt must be after startAt";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
