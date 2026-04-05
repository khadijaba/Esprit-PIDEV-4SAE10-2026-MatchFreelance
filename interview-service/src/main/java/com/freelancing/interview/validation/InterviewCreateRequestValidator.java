package com.freelancing.interview.validation;

import com.freelancing.interview.dto.InterviewCreateRequestDTO;
import com.freelancing.interview.enums.MeetingMode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * Validates: either slotId is set, or both startAt and endAt are set.
 * When mode is ONLINE, meetingUrl must be non-blank.
 * When mode is FACE_TO_FACE, addressLine and city must be non-blank.
 */
public class InterviewCreateRequestValidator implements ConstraintValidator<ValidInterviewCreateRequest, InterviewCreateRequestDTO> {

    @Override
    public boolean isValid(InterviewCreateRequestDTO req, ConstraintValidatorContext ctx) {
        if (req == null) return true;

        // Either slotId OR (startAt AND endAt)
        boolean hasSlot = req.getSlotId() != null;
        boolean hasManualTimes = req.getStartAt() != null && req.getEndAt() != null;
        if (!hasSlot && !hasManualTimes) {
            addMessage(ctx, "Provide either slotId or both startAt and endAt");
            return false;
        }
        if (hasSlot && hasManualTimes) {
            addMessage(ctx, "Provide either slotId or startAt/endAt, not both");
            return false;
        }
        if (hasManualTimes && !req.getEndAt().isAfter(req.getStartAt())) {
            addMessage(ctx, "endAt must be after startAt");
            return false;
        }

        MeetingMode mode = req.getMode() != null ? req.getMode() : MeetingMode.ONLINE;
        if (mode == MeetingMode.FACE_TO_FACE) {
            if (!StringUtils.hasText(req.getAddressLine()) || !StringUtils.hasText(req.getCity())) {
                addMessage(ctx, "addressLine and city are required for FACE_TO_FACE mode");
                return false;
            }
        }

        return true;
    }

    private void addMessage(ConstraintValidatorContext ctx, String message) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
