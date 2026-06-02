package ar.edu.itba.paw.webapp.form;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.Test;

class CreateTournamentFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validTournamentFormPassesBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankTitleFailsBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();
        form.setTitle("");

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "title"));
    }

    @Test
    void negativePriceFailsBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();
        form.setPricePerPlayer(BigDecimal.valueOf(-1));

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "pricePerPlayer"));
    }

    @Test
    void onePlayerTeamSizePassesBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();
        form.setTeamSize(1);

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertFalse(hasViolation(violations, "teamSize"));
    }

    @Test
    void incompleteCoordinatesFailBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();
        form.setLatitude(-34.6);
        form.setLongitude(null);

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "longitude"));
    }

    @Test
    void invalidBracketSizeFailsBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();
        form.setBracketSize(6);

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "bracketSize"));
    }

    @Test
    void invalidTeamSizeForSportFailsBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();
        form.setSport(Sport.FOOTBALL);
        form.setTeamSize(2);

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "teamSize"));
    }

    @Test
    void noJoinModeFailsBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();
        form.setAllowSoloSignup(false);
        form.setAllowTeamDraft(false);

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "allowSoloSignup"));
    }

    @Test
    void closedBeforeOpenFailsBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();
        form.setRegistrationClosesDate(form.getRegistrationOpensDate());
        form.setRegistrationClosesTime(form.getRegistrationOpensTime().minusMinutes(1));

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "registrationClosesTime"));
    }

    @Test
    void outOfRangeCoordinatesFailBeanValidation() {
        // 1. Arrange
        final CreateTournamentForm form = validForm();
        form.setLatitude(120.0);
        form.setLongitude(-58.4);

        // 2. Exercise
        final Set<ConstraintViolation<CreateTournamentForm>> violations = validator.validate(form);

        // 3. Assert
        assertTrue(hasViolation(violations, "latitude"));
    }

    private static CreateTournamentForm validForm() {
        final CreateTournamentForm form = new CreateTournamentForm();
        form.setTitle("City Padel Cup");
        form.setDescription("Open city tournament");
        form.setAddress("Downtown Club");
        form.setSport(Sport.PADEL);
        form.setLatitude(-34.6);
        form.setLongitude(-58.4);
        form.setRegistrationOpensDate(LocalDate.now().plusDays(1));
        form.setRegistrationClosesDate(LocalDate.now().plusDays(10));
        form.setBracketSize(8);
        form.setTeamSize(1);
        form.setPricePerPlayer(BigDecimal.ZERO);
        form.setAllowSoloSignup(true);
        return form;
    }

    private static boolean hasViolation(
            final Set<ConstraintViolation<CreateTournamentForm>> violations,
            final String property) {
        return violations.stream()
                .anyMatch(violation -> property.equals(violation.getPropertyPath().toString()));
    }
}
