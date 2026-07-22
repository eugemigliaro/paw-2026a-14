package ar.edu.itba.paw.webapp.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public final class ValidatorTestUtils {

    private ValidatorTestUtils() {}

    public static LocalValidatorFactoryBean validator(
            final ConstraintValidator<?, ?>... validators) {
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource());
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

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
