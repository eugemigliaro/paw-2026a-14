package ar.edu.itba.paw.webapp.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = BracketManualPairingsFormValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBracketManualPairingsForm {

    String message() default "{tournament.bracket.manualPairings.validation.invalidSelection}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
