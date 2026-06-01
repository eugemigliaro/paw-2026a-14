package ar.edu.itba.paw.webapp.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = BracketPublishFormValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBracketPublishForm {

    String message() default "{tournament.bracket.schedule.validation.invalidRange}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
