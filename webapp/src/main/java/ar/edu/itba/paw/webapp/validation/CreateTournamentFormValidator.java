package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.webapp.form.CreateTournamentForm;
import java.time.Instant;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CreateTournamentFormValidator
        implements ConstraintValidator<ValidCreateTournamentForm, CreateTournamentForm> {

    private static final int MIN_BRACKET_SIZE = 4;
    private static final int MEDIUM_BRACKET_SIZE = 8;
    private static final int MAX_BRACKET_SIZE = 16;

    @Override
    public boolean isValid(
            final CreateTournamentForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        valid = validateSport(form, context) && valid;
        valid = validateBracketSize(form, context) && valid;
        valid = validateTeamSize(form, context) && valid;
        valid = validateJoinMode(form, context) && valid;
        valid = validateCoordinates(form, context) && valid;
        valid = validateRegistrationWindow(form, context) && valid;

        return valid;
    }

    private static boolean validateSport(
            final CreateTournamentForm form, final ConstraintValidatorContext context) {
        if (form.getSport() == null) {
            reject(context, "sport", "{CreateTournamentForm.sport.NotNull}");
            return false;
        }
        return true;
    }

    private static boolean validateBracketSize(
            final CreateTournamentForm form, final ConstraintValidatorContext context) {
        if (form.getBracketSize() == null) {
            return true;
        }
        if (form.getBracketSize() != MIN_BRACKET_SIZE
                && form.getBracketSize() != MEDIUM_BRACKET_SIZE
                && form.getBracketSize() != MAX_BRACKET_SIZE) {
            reject(context, "bracketSize", "{CreateTournamentForm.bracketSize.Valid}");
            return false;
        }
        return true;
    }

    private static boolean validateTeamSize(
            final CreateTournamentForm form, final ConstraintValidatorContext context) {
        if (form.getSport() == null || form.getTeamSize() == null) {
            return true;
        }

        final int teamSize = form.getTeamSize();
        boolean supported = false;
        if (form.getSport() == Sport.PADEL || form.getSport() == Sport.TENNIS) {
            supported = teamSize == 1 || teamSize == 2;
        } else if (form.getSport() == Sport.FOOTBALL) {
            supported = teamSize == 5 || teamSize == 7 || teamSize == 8 || teamSize == 11;
        } else if (form.getSport() == Sport.BASKETBALL) {
            supported = teamSize == 3 || teamSize == 5;
        } else if (form.getSport() == Sport.OTHER) {
            supported = teamSize >= 1 && teamSize <= 11;
        }

        if (!supported) {
            reject(context, "teamSize", "{CreateTournamentForm.teamSize.ValidForSport}");
            return false;
        }
        return true;
    }

    private static boolean validateJoinMode(
            final CreateTournamentForm form, final ConstraintValidatorContext context) {
        if (!form.isAllowSoloSignup() && !form.isAllowTeamDraft()) {
            reject(context, "allowSoloSignup", "{CreateTournamentForm.joinMode.Required}");
            return false;
        }
        return true;
    }

    private static boolean validateCoordinates(
            final CreateTournamentForm form, final ConstraintValidatorContext context) {
        final Double latitude = form.getLatitude();
        final Double longitude = form.getLongitude();
        final boolean hasLatitude = latitude != null;
        final boolean hasLongitude = longitude != null;

        if (hasLatitude != hasLongitude) {
            reject(
                    context,
                    hasLatitude ? "longitude" : "latitude",
                    "{CreateTournamentForm.coordinates.Pair}");
            return false;
        }
        if (!hasLatitude) {
            return true;
        }

        boolean valid = true;
        if (latitude < -90 || latitude > 90) {
            reject(context, "latitude", "{CreateTournamentForm.coordinates.Invalid}");
            valid = false;
        }
        if (longitude < -180 || longitude > 180) {
            reject(context, "longitude", "{CreateTournamentForm.coordinates.Invalid}");
            valid = false;
        }
        return valid;
    }

    private static boolean validateRegistrationWindow(
            final CreateTournamentForm form, final ConstraintValidatorContext context) {
        if (form.getRegistrationOpensDate() == null
                || form.getRegistrationOpensTime() == null
                || form.getRegistrationClosesDate() == null
                || form.getRegistrationClosesTime() == null) {
            return true;
        }

        final Instant opensAt =
                toInstant(
                        form.getRegistrationOpensDate(),
                        form.getRegistrationOpensTime(),
                        form.getTz());
        final Instant closesAt =
                toInstant(
                        form.getRegistrationClosesDate(),
                        form.getRegistrationClosesTime(),
                        form.getTz());

        if (!closesAt.isAfter(opensAt)) {
            reject(
                    context,
                    "registrationClosesTime",
                    "{CreateTournamentForm.registrationClosesTime.AfterOpen}");
            return false;
        }
        return true;
    }

    private static Instant toInstant(
            final java.time.LocalDate date, final java.time.LocalTime time, final String timezone) {
        return PlatformTime.toInstant(date, time);
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
