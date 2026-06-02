package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.webapp.form.BracketManualPairingsForm;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class BracketManualPairingsFormValidator
        implements ConstraintValidator<ValidBracketManualPairingsForm, BracketManualPairingsForm> {

    @Override
    public boolean isValid(
            final BracketManualPairingsForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }

        final List<Long> teamIds = form.getTeamIds();
        if (teamIds == null || teamIds.isEmpty() || form.getExpectedTeamCount() == null) {
            reject(context, "{tournament.bracket.manualPairings.validation.teamCount}");
            return false;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (teamIds.size() != form.getExpectedTeamCount()) {
            reject(context, "teamIds", "{tournament.bracket.manualPairings.validation.teamCount}");
            reject(context, "{tournament.bracket.manualPairings.validation.teamCount}");
            valid = false;
        }

        final Set<Long> uniqueTeamIds = new HashSet<>();
        for (final Long teamId : teamIds) {
            if (teamId == null || !uniqueTeamIds.add(teamId)) {
                reject(
                        context,
                        "teamIds",
                        "{tournament.bracket.manualPairings.validation.invalidSelection}");
                reject(context, "{tournament.bracket.manualPairings.validation.invalidSelection}");
                valid = false;
                break;
            }
        }

        return valid;
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
