package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.services.UserService;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private final UserService userService;

    private boolean mustExist;

    @Autowired
    public UsernameValidator(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public void initialize(final ValidUsername annotation) {
        this.mustExist = annotation.mustExist();
    }

    @Override
    public boolean isValid(final String username, final ConstraintValidatorContext context) {

        if (username == null || username.isBlank()) {
            return true;
        }

        final boolean exists = userService.findByUsername(username).isPresent();

        return mustExist ? exists : !exists;
    }
}
