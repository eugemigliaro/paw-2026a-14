package ar.edu.itba.paw.webapp.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = RecurrenceSelectionValidator.class)
public @interface ValidRecurrenceSelection {

    String message() default "{CreateEventForm.recurrence.Valid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
