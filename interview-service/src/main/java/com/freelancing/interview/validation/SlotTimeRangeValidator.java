package com.freelancing.interview.validation;

import com.freelancing.interview.dto.AvailabilitySlotCreateRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SlotTimeRangeValidator implements ConstraintValidator<ValidSlotTimeRange, AvailabilitySlotCreateRequestDTO> {

    @Override
    public boolean isValid(AvailabilitySlotCreateRequestDTO req, ConstraintValidatorContext ctx) {
        if (req == null || req.getStartAt() == null || req.getEndAt() == null) return true;
        if (!req.getEndAt().isAfter(req.getStartAt())) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("endAt must be after startAt").addConstraintViolation();
            return false;
        }
        return true;
    }
}
