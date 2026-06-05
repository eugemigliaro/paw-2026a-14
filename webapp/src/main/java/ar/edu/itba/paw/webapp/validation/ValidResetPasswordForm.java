package ar.edu.itba.paw.webapp.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = ResetPasswordFormValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidResetPasswordForm {

    String message() default "{auth.invalid_credentials}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
