package ar.edu.itba.paw.webapp.form;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.Test;

class CreateEventFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nonRecurringFormAllowsBlankRecurrenceFields() {
        // 1. Arrange
        final CreateEventForm form = validForm();
        form.setRecurring(false);
        form.setRecurrenceFrequency("yearly");

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        assertFalse(hasViolation(violations, "recurrenceFrequency"));
        assertFalse(hasViolation(violations, "recurrenceEndMode"));
    }

    @Test
    void recurringFormRejectsUnsupportedFrequency() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();
        form.setRecurrenceFrequency("yearly");

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "recurrenceFrequency"));
    }

    @Test
    void recurringFormAcceptsWeeklyOccurrenceCountEndMode() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        assertFalse(hasViolation(violations, "recurrenceFrequency"));
        assertFalse(hasViolation(violations, "recurrenceOccurrenceCount"));
    }

    @Test
    void recurringFormAcceptsWeeklyUntilDateEndMode() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();
        form.setRecurrenceEndMode("until_date");
        form.setRecurrenceOccurrenceCount(null);
        form.setRecurrenceUntilDate(form.getEventDate().plusWeeks(2));

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        assertFalse(hasViolation(violations, "recurrenceEndMode"));
        assertFalse(hasViolation(violations, "recurrenceUntilDate"));
    }

    @Test
    void recurringFormRejectsUntilDateBeforeNextOccurrence() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();
        form.setRecurrenceEndMode("until_date");
        form.setRecurrenceOccurrenceCount(null);
        form.setRecurrenceUntilDate(form.getEventDate().plusDays(2));

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "recurrenceUntilDate"));
    }

    @Test
    void recurringFormRejectsUntilDateThatCreatesTooManyOccurrences() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();
        form.setRecurrenceFrequency("daily");
        form.setRecurrenceEndMode("until_date");
        form.setRecurrenceOccurrenceCount(null);
        form.setRecurrenceUntilDate(form.getEventDate().plusDays(60));

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "recurrenceUntilDate"));
    }

    @Test
    void recurringFormRejectsMixedEndConditions() {
        // 1. Arrange
        final CreateEventForm form = validRecurringForm();
        form.setRecurrenceUntilDate(form.getEventDate().plusWeeks(2));

        // 2. Exercise
        final Set<ConstraintViolation<CreateEventForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "recurrenceUntilDate"));
    }

    private static CreateEventForm validForm() {
        final CreateEventForm form = new CreateEventForm();
        form.setTitle("Weekly Padel");
        form.setAddress("Downtown Club");
        form.setVisibility("public");
        form.setJoinPolicy("direct");
        form.setEventDate(LocalDate.now().plusDays(14));
        form.setEndDate(LocalDate.now().plusDays(14));
        return form;
    }

    private static CreateEventForm validRecurringForm() {
        final CreateEventForm form = validForm();
        form.setRecurring(true);
        form.setRecurrenceFrequency("weekly");
        form.setRecurrenceEndMode("occurrence_count");
        form.setRecurrenceOccurrenceCount(3);
        return form;
    }

    private static boolean hasViolation(
            final Set<ConstraintViolation<CreateEventForm>> violations, final String property) {
        return violations.stream()
                .anyMatch(violation -> property.equals(violation.getPropertyPath().toString()));
    }
}
