package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.RecurrenceEndMode;
import ar.edu.itba.paw.models.types.RecurrenceFrequency;
import java.time.LocalDate;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CreateEventFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nonRecurringFormAllowsBlankRecurrenceFields() {
        // 1. Arrange
        final CreateEventForm form = validForm();
        form.setRecurring(false);
        form.setRecurrenceFrequency(RecurrenceFrequency.MONTHLY);

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        Assertions.assertFalse(hasViolation(violations, "recurrenceFrequency"));
        Assertions.assertFalse(hasViolation(violations, "recurrenceEndMode"));
    }

    @Test
    void recurringFormAcceptsWeeklyOccurrenceCountEndMode() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        Assertions.assertFalse(hasViolation(violations, "recurrenceFrequency"));
        Assertions.assertFalse(hasViolation(violations, "recurrenceOccurrenceCount"));
    }

    @Test
    void recurringFormAcceptsWeeklyUntilDateEndMode() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();
        form.setRecurrenceEndMode(RecurrenceEndMode.UNTIL_DATE);
        form.setRecurrenceOccurrenceCount(null);
        form.setRecurrenceUntilDate(form.getEventDate().plusWeeks(2));

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        Assertions.assertFalse(hasViolation(violations, "recurrenceEndMode"));
        Assertions.assertFalse(hasViolation(violations, "recurrenceUntilDate"));
    }

    @Test
    void recurringFormRejectsUntilDateBeforeNextOccurrence() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();
        form.setRecurrenceEndMode(RecurrenceEndMode.UNTIL_DATE);
        form.setRecurrenceOccurrenceCount(null);
        form.setRecurrenceUntilDate(form.getEventDate().plusDays(2));

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        Assertions.assertTrue(hasViolation(violations, "recurrenceUntilDate"));
    }

    @Test
    void recurringFormRejectsUntilDateThatCreatesTooManyOccurrences() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();
        form.setRecurrenceFrequency(RecurrenceFrequency.DAILY);
        form.setRecurrenceEndMode(RecurrenceEndMode.UNTIL_DATE);
        form.setRecurrenceOccurrenceCount(null);
        form.setRecurrenceUntilDate(form.getEventDate().plusDays(60));

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        Assertions.assertTrue(hasViolation(violations, "recurrenceUntilDate"));
    }

    @Test
    void recurringFormRejectsMixedEndConditions() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();
        form.setRecurrenceUntilDate(form.getEventDate().plusWeeks(2));

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        Assertions.assertTrue(hasViolation(violations, "recurrenceUntilDate"));
    }

    @Test
    void publicFormRejectsInviteOnlyJoinPolicy() {
        // 1. Arrange
        final CreateEventForm form = validForm();
        form.setVisibility(EventVisibility.PUBLIC);
        form.setJoinPolicy(EventJoinPolicy.INVITE_ONLY);

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        Assertions.assertTrue(hasViolation(violations, "joinPolicy"));
    }

    @Test
    void privateFormAllowsBlankJoinPolicy() {
        // 1. Arrange
        final CreateEventForm form = validForm();
        form.setVisibility(EventVisibility.PRIVATE);
        form.setJoinPolicy(null);

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        Assertions.assertFalse(hasViolation(violations, "joinPolicy"));
    }

    private static CreateEventForm validForm() {
        final CreateEventForm form = new CreateEventForm();
        form.setTitle("Weekly Padel");
        form.setAddress("Downtown Club");
        form.setVisibility(EventVisibility.PUBLIC);
        form.setJoinPolicy(EventJoinPolicy.DIRECT);
        form.setEventDate(LocalDate.now().plusDays(14));
        form.setEndDate(LocalDate.now().plusDays(14));
        return form;
    }

    private static CreateEventForm validRecurringForm() {
        final CreateEventForm form = validForm();
        form.setRecurring(true);
        form.setRecurrenceFrequency(RecurrenceFrequency.WEEKLY);
        form.setRecurrenceEndMode(RecurrenceEndMode.OCCURRENCE_COUNT);
        form.setRecurrenceOccurrenceCount(3);
        return form;
    }

    private static boolean hasViolation(
            final Set<ConstraintViolation<CreateEventForm>> violations, final String property) {
        return violations.stream()
                .anyMatch(violation -> property.equals(violation.getPropertyPath().toString()));
    }
}
