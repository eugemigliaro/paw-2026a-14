package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.models.query.EventFilter;
import ar.edu.itba.paw.webapp.form.SearchForm;
import java.time.LocalDate;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SearchFormValidator implements ConstraintValidator<ValidSearchForm, SearchForm> {

    @Override
    public boolean isValid(final SearchForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        valid = validateDateRange(form, context) && valid;
        valid = validatePriceRange(form, context) && valid;

        return valid;
    }

    private static boolean validateDateRange(
            final SearchForm form, final ConstraintValidatorContext context) {

        if (form.getStartDate() == null && form.getEndDate() == null) {
            return true;
        }

        final LocalDate today = LocalDate.now();

        if (form.getFilter() == EventFilter.PAST) {
            if (form.getStartDate() != null && form.getStartDate().isAfter(today)) {
                reject(context, "startDate", "{SearchForm.error.startDateInFuture}");
                return false;
            }
            if (form.getEndDate() != null && form.getEndDate().isAfter(today)) {
                reject(context, "endDate", "{SearchForm.error.endDateInFuture}");
                return false;
            }
        } else if (form.getFilter() == EventFilter.UPCOMING) {
            if (form.getStartDate() != null && form.getStartDate().isBefore(today)) {
                reject(context, "startDate", "{SearchForm.error.startDateInPast}");
                return false;
            }
            if (form.getEndDate() != null && form.getEndDate().isBefore(today)) {
                reject(context, "endDate", "{SearchForm.error.endDateInPast}");
                return false;
            }
        }

        if (form.getStartDate() != null
                && form.getEndDate() != null
                && form.getEndDate().isBefore(form.getStartDate())) {
            reject(context, "endDate", "{SearchForm.error.endBeforeStart}");
            return false;
        }

        return true;
    }

    private static boolean validatePriceRange(
            final SearchForm form, final ConstraintValidatorContext context) {

        if (form.getMinPrice() == null && form.getMaxPrice() == null) {
            return true;
        }

        if (form.getMinPrice() != null
                && form.getMaxPrice() != null
                && form.getMaxPrice().compareTo(form.getMinPrice()) < 0) {
            reject(context, "maxPrice", "{SearchForm.error.maxPriceLessThanMin}");
            return false;
        }

        return true;
    }

    private static void reject(
            final ConstraintValidatorContext context,
            final String property,
            final String messageTemplate) {
        context.buildConstraintViolationWithTemplate(messageTemplate)
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}
