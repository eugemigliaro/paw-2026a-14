package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.webapp.form.CreateEventForm;
import java.time.Instant;
import java.time.ZoneId;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CreateEventFormValidator
        implements ConstraintValidator<ValidCreateEventForm, CreateEventForm> {

    @Override
    public boolean isValid(final CreateEventForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        valid = validateSchedule(form, context) && valid;
        valid = validateVisibilityAndJoinPolicy(form, context) && valid;
        valid = validateCoordinates(form, context) && valid;

        return valid;
    }

    private static boolean validateSchedule(
            final CreateEventForm form, final ConstraintValidatorContext context) {

        if (form.getEventDate() == null
                || form.getEventTime() == null
                || form.getEndDate() == null
                || form.getEndTime() == null) {
            return true; // let @NotNull handle it
        }

        if (!isScheduledInFuture(form)) {
            reject(context, "eventTime", "{match.schedule.error.startsAtPast}");
            return false;
        }
        if (!isEndAfterStart(form)) {
            reject(context, "endTime", "{match.schedule.error.endBeforeStart}");
            return false;
        }
        return true;
    }

    private static boolean isScheduledInFuture(final CreateEventForm form) {
        final ZoneId timezone =
                form.getTimezone() == null ? ZoneId.systemDefault() : form.getTimezone();
        final Instant startsAt =
                form.getEventDate().atTime(form.getEventTime()).atZone(timezone).toInstant();
        return startsAt.isAfter(Instant.now());
    }

    private static boolean isEndAfterStart(final CreateEventForm form) {
        final ZoneId timezone =
                form.getTimezone() == null ? ZoneId.systemDefault() : form.getTimezone();
        final Instant startsAt =
                form.getEventDate().atTime(form.getEventTime()).atZone(timezone).toInstant();
        final Instant endsAt =
                form.getEndDate().atTime(form.getEndTime()).atZone(timezone).toInstant();
        return endsAt.isAfter(startsAt);
    }

    private boolean validateVisibilityAndJoinPolicy(
            final CreateEventForm form, final ConstraintValidatorContext context) {

        if (form.getVisibility() == null) {
            reject(context, "visibility", "{host.validation.visibility.invalid}");
            return false;
        }

        if (EventVisibility.PRIVATE == form.getVisibility()) {
            // Private events are always invite_only; no join policy selection needed.
            return true;
        }

        if (form.getJoinPolicy() == null) {
            reject(context, "joinPolicy", "{host.validation.joinPolicy.required}");
            return false;
        }

        return true;
    }

    private boolean validateCoordinates(
            final CreateEventForm form, final ConstraintValidatorContext context) {
        final boolean hasLatitude = form.getLatitude() != null;
        final boolean hasLongitude = form.getLongitude() != null;

        if (hasLatitude != hasLongitude) {
            reject(
                    context,
                    hasLatitude ? "longitude" : "latitude",
                    "{CreateEventForm.coordinates.Pair}");
            return false;
        }
        if (!hasLatitude) {
            return true;
        }

        if (form.getLatitude() < -90 || form.getLatitude() > 90) {
            reject(context, "latitude", "{CreateEventForm.coordinates.Invalid}");
            return false;
        }
        if (form.getLongitude() < -180 || form.getLongitude() > 180) {
            reject(context, "longitude", "{CreateEventForm.coordinates.Invalid}");
            return false;
        }

        return true;
    }

    private static void reject(
            final ConstraintValidatorContext context,
            final String property,
            final String messageTemplate) {
        context.buildConstraintViolationWithTemplate(messageTemplate)
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}
