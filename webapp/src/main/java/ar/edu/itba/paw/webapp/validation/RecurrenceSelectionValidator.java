package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.models.RecurrenceEndMode;
import ar.edu.itba.paw.models.RecurrenceFrequency;
import ar.edu.itba.paw.webapp.form.CreateEventForm;
import java.time.LocalDate;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RecurrenceSelectionValidator
        implements ConstraintValidator<ValidRecurrenceSelection, CreateEventForm> {

    static final int MIN_OCCURRENCES = 2;
    static final int MAX_OCCURRENCES = 52;

    @Override
    public boolean isValid(final CreateEventForm form, final ConstraintValidatorContext context) {
        if (form == null || !form.isRecurring()) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        final RecurrenceFrequency frequency =
                RecurrenceFrequency.fromValue(form.getRecurrenceFrequency()).orElse(null);
        if (frequency == null) {
            reject(
                    context,
                    "recurrenceFrequency",
                    isBlank(form.getRecurrenceFrequency())
                            ? "{CreateEventForm.recurrenceFrequency.NotBlank}"
                            : "{CreateEventForm.recurrenceFrequency.Valid}");
            valid = false;
        }

        final RecurrenceEndMode endMode =
                RecurrenceEndMode.fromValue(form.getRecurrenceEndMode()).orElse(null);
        if (endMode == null) {
            reject(
                    context,
                    "recurrenceEndMode",
                    isBlank(form.getRecurrenceEndMode())
                            ? "{CreateEventForm.recurrenceEndMode.NotBlank}"
                            : "{CreateEventForm.recurrenceEndMode.Valid}");
            return false;
        }

        switch (endMode) {
            case UNTIL_DATE:
                if (form.getRecurrenceOccurrenceCount() != null) {
                    reject(
                            context,
                            "recurrenceOccurrenceCount",
                            "{CreateEventForm.recurrenceEndMode.Exclusive}");
                    valid = false;
                }
                valid = validateUntilDate(form, context, frequency) && valid;
                break;
            case OCCURRENCE_COUNT:
                if (form.getRecurrenceUntilDate() != null) {
                    reject(
                            context,
                            "recurrenceUntilDate",
                            "{CreateEventForm.recurrenceEndMode.Exclusive}");
                    valid = false;
                }
                valid = validateOccurrenceCount(form, context) && valid;
                break;
            default:
                valid = false;
                break;
        }

        return valid;
    }

    private static boolean validateUntilDate(
            final CreateEventForm form,
            final ConstraintValidatorContext context,
            final RecurrenceFrequency frequency) {
        if (form.getRecurrenceUntilDate() == null) {
            reject(context, "recurrenceUntilDate", "{CreateEventForm.recurrenceUntilDate.NotNull}");
            return false;
        }
        if (form.getEventDate() == null || frequency == null) {
            return true;
        }

        final LocalDate firstNextDate = nextDate(form.getEventDate(), frequency);
        if (form.getRecurrenceUntilDate().isBefore(firstNextDate)) {
            reject(context, "recurrenceUntilDate", "{CreateEventForm.recurrenceUntilDate.TooSoon}");
            return false;
        }
        if (countOccurrencesUntilDate(form.getEventDate(), form.getRecurrenceUntilDate(), frequency)
                > MAX_OCCURRENCES) {
            reject(context, "recurrenceUntilDate", "{CreateEventForm.recurrenceUntilDate.Max}");
            return false;
        }

        return true;
    }

    private static boolean validateOccurrenceCount(
            final CreateEventForm form, final ConstraintValidatorContext context) {
        if (form.getRecurrenceOccurrenceCount() == null) {
            reject(
                    context,
                    "recurrenceOccurrenceCount",
                    "{CreateEventForm.recurrenceOccurrenceCount.NotNull}");
            return false;
        }
        if (form.getRecurrenceOccurrenceCount() < MIN_OCCURRENCES) {
            reject(
                    context,
                    "recurrenceOccurrenceCount",
                    "{CreateEventForm.recurrenceOccurrenceCount.Min}");
            return false;
        }
        if (form.getRecurrenceOccurrenceCount() > MAX_OCCURRENCES) {
            reject(
                    context,
                    "recurrenceOccurrenceCount",
                    "{CreateEventForm.recurrenceOccurrenceCount.Max}");
            return false;
        }
        return true;
    }

    private static LocalDate nextDate(
            final LocalDate eventDate, final RecurrenceFrequency frequency) {
        return dateAtIndex(eventDate, frequency, 1);
    }

    private static int countOccurrencesUntilDate(
            final LocalDate eventDate,
            final LocalDate untilDate,
            final RecurrenceFrequency frequency) {
        int count = 0;
        int index = 0;
        LocalDate occurrenceDate = eventDate;
        while (!occurrenceDate.isAfter(untilDate)) {
            count++;
            if (count > MAX_OCCURRENCES) {
                return count;
            }
            index++;
            occurrenceDate = dateAtIndex(eventDate, frequency, index);
        }
        return count;
    }

    private static LocalDate dateAtIndex(
            final LocalDate eventDate, final RecurrenceFrequency frequency, final int index) {
        switch (frequency) {
            case DAILY:
                return eventDate.plusDays(index);
            case MONTHLY:
                return eventDate.plusMonths(index);
            case WEEKLY:
            default:
                return eventDate.plusWeeks(index);
        }
    }

    private static void reject(
            final ConstraintValidatorContext context,
            final String property,
            final String messageTemplate) {
        context.buildConstraintViolationWithTemplate(messageTemplate)
                .addPropertyNode(property)
                .addConstraintViolation();
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
