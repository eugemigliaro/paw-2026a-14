package ar.edu.itba.paw.webapp.form;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static CreateTournamentForm validForm() {
        final CreateTournamentForm form = new CreateTournamentForm();
        form.setTitle("City Padel Cup");
        form.setDescription("Open city tournament");
        form.setAddress("Downtown Club");
        form.setSport("padel");
        form.setStartDate(LocalDate.now().plusDays(14));
        form.setEndDate(LocalDate.now().plusDays(14));
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
