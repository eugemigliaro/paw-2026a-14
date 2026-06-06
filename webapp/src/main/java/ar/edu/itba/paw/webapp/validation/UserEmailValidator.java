package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.services.UserService;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserEmailValidator implements ConstraintValidator<ValidUserEmail, String> {

    private final UserService userService;

    private boolean mustExist;

    @Autowired
    public UserEmailValidator(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public void initialize(final ValidUserEmail annotation) {
        this.mustExist = annotation.mustExist();
    }

    @Override
    public boolean isValid(final String email, final ConstraintValidatorContext context) {

        if (email == null || email.isBlank()) {
            return true;
        }

        final boolean exists = userService.findByEmail(email).isPresent();

        return mustExist ? exists : !exists;
    }
}
