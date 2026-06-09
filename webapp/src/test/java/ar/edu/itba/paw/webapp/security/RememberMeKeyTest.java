package ar.edu.itba.paw.webapp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class RememberMeKeyTest {

    @Test
    void fromConfiguredValueAcceptsStrongExternalKey() {
        // 1. Arrange
        final String configuredValue = "  0123456789abcdef0123456789abcdef  ";

        // 2. Exercise
        final RememberMeKey key = RememberMeKey.fromConfiguredValue(configuredValue);

        // 3. Assert
        assertEquals("0123456789abcdef0123456789abcdef", key.value());
    }

    @Test
    void fromConfiguredValueRejectsBlankKey() {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        assertThrows(IllegalArgumentException.class, () -> RememberMeKey.fromConfiguredValue(" "));
    }

    @Test
    void fromConfiguredValueRejectsShortKey() {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> RememberMeKey.fromConfiguredValue("short-secret"));
    }

    @Test
    void fromConfiguredValueRejectsUnresolvedPlaceholder() {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> RememberMeKey.fromConfiguredValue("${security.rememberMe.key}"));
    }

    @Test
    void fromConfiguredValueRejectsExamplePlaceholder() {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> RememberMeKey.fromConfiguredValue("<generate-a-strong-random-secret>"));
    }

    @Test
    void fromConfiguredValueRejectsPlaceholderWithTurkishDefaultLocale() {
        // 1. Arrange
        final Locale originalLocale = Locale.getDefault();

        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            // 2. Exercise + 3. Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            RememberMeKey.fromConfiguredValue(
                                    "PLACEHOLDER-0123456789abcdef0123456789abcdef"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
