package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.webapp.form.BracketPublishForm;
import ar.edu.itba.paw.webapp.form.BracketPublishScheduleForm;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class BracketPublishFormValidator
        implements ConstraintValidator<ValidBracketPublishForm, BracketPublishForm> {

    @Override
    public boolean isValid(
            final BracketPublishForm form, final ConstraintValidatorContext context) {
        if (form == null || form.getSchedules() == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        final Instant now = Instant.now();
        Instant previousRoundEnd = null;
        Integer previousRoundNumber = null;

        final List<BracketPublishScheduleForm> schedules = form.getSchedules();
        if (schedules.isEmpty()) {
            reject(context, "{tournament.bracket.schedule.validation.required}");
            return false;
        }

        for (int index = 0; index < schedules.size(); index++) {
            final BracketPublishScheduleForm schedule = schedules.get(index);
            if (schedule == null) {
                reject(context, "{tournament.bracket.schedule.validation.required}");
                continue;
            }
            if (schedule.getMatchId() == null
                    || schedule.getRoundNumber() == null
                    || schedule.getStartDate() == null
                    || schedule.getStartTime() == null
                    || schedule.getEndDate() == null
                    || schedule.getEndTime() == null) {
                reject(context, "{tournament.bracket.schedule.validation.required}");
                continue;
            }

            final Instant startsAt =
                    toInstant(schedule.getStartDate(), schedule.getStartTime(), form.getTz());
            final Instant endsAt =
                    toInstant(schedule.getEndDate(), schedule.getEndTime(), form.getTz());

            if (endsAt != null && startsAt != null && !endsAt.isAfter(startsAt)) {
                reject(
                        context,
                        "schedules[" + index + "].endTime",
                        "{tournament.bracket.schedule.validation.invalidRange}");
                reject(context, "{tournament.bracket.schedule.validation.invalidRange}");
                valid = false;
            }
            if (startsAt != null && startsAt.isBefore(now)) {
                reject(
                        context,
                        "schedules[" + index + "].startDate",
                        "{tournament.bracket.schedule.validation.beforeNow}");
                reject(context, "{tournament.bracket.schedule.validation.beforeNow}");
                valid = false;
            }

            final int roundNumber = schedule.getRoundNumber();
            if (previousRoundNumber == null || previousRoundNumber.intValue() != roundNumber) {
                previousRoundNumber = roundNumber;
                previousRoundEnd =
                        schedules.stream()
                                .limit(index)
                                .filter(other -> other != null && other.getRoundNumber() != null)
                                .filter(other -> other.getRoundNumber() < roundNumber)
                                .map(
                                        other ->
                                                toInstant(
                                                        other.getEndDate(),
                                                        other.getEndTime(),
                                                        form.getTz()))
                                .filter(value -> value != null)
                                .max(Comparator.naturalOrder())
                                .orElse(previousRoundEnd);
            }

            if (previousRoundEnd != null && startsAt.isBefore(previousRoundEnd)) {
                reject(
                        context,
                        "schedules[" + index + "].startDate",
                        "{tournament.bracket.schedule.validation.roundOrder}");
                reject(context, "{tournament.bracket.schedule.validation.roundOrder}");
                valid = false;
            }
        }

        return valid;
    }

    private static Instant toInstant(
            final java.time.LocalDate date, final java.time.LocalTime time, final ZoneId zoneId) {
        return LocalDateTime.of(date, time).atZone(zoneId).toInstant();
    }

    private static void reject(
            final ConstraintValidatorContext context, final String messageTemplate) {
        context.buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation();
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
