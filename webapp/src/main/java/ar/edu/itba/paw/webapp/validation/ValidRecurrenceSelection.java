package ar.edu.itba.paw.webapp.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RecurrenceSelectionValidator.class)
public @interface ValidRecurrenceSelection {

    String message() default "{CreateEventForm.recurrence.Valid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
