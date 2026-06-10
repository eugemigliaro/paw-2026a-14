package ar.edu.itba.paw.webapp.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import org.springframework.context.MessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public final class ValidatorTestUtils {

    private ValidatorTestUtils() {}

    public static LocalValidatorFactoryBean validator(
            final ConstraintValidator<?, ?>... validators) {
        return validator(null, validators);
    }

    public static LocalValidatorFactoryBean validator(
            final MessageSource messageSource, final ConstraintValidator<?, ?>... validators) {
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        if (messageSource != null) {
            validator.setValidationMessageSource(messageSource);
        }
        validator.setConstraintValidatorFactory(validatorFactory(validators));
        validator.afterPropertiesSet();
        return validator;
    }

    private static ConstraintValidatorFactory validatorFactory(
            final ConstraintValidator<?, ?>... validators) {
        final Map<Class<?>, ConstraintValidator<?, ?>> validatorsByType = new LinkedHashMap<>();
        for (final ConstraintValidator<?, ?> validator : validators) {
            validatorsByType.put(validator.getClass(), validator);
        }
        return new ConstraintValidatorFactory() {
            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
                final ConstraintValidator<?, ?> validator = validatorsByType.get(key);
                if (validator != null) {
                    return key.cast(validator);
                }
                try {
                    return key.getDeclaredConstructor().newInstance();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void releaseInstance(final ConstraintValidator<?, ?> instance) {}
        };
    }
}
