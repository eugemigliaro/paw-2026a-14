package ar.edu.itba.paw.webapp.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = CreateEventFormValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCreateEventForm {

    String message() default "{CreateEventForm.Valid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
