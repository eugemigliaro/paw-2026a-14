package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.webapp.form.RegisterForm;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RegisterFormValidator implements ConstraintValidator<ValidRegisterForm, RegisterForm> {

    @Override
    public boolean isValid(final RegisterForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }

        if (form.getPassword() != null
                && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{auth.validation.passwordMismatch}")
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
