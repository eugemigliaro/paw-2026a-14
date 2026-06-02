package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.webapp.form.ResetPasswordForm;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ResetPasswordFormValidator
        implements ConstraintValidator<ValidResetPasswordForm, ResetPasswordForm> {

    @Override
    public boolean isValid(final ResetPasswordForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{auth.validation.passwordMismatch}")
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
