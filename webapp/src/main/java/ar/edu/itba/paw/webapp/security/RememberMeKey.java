package ar.edu.itba.paw.webapp.security;

import java.util.Locale;

public final class RememberMeKey {

    private static final int MINIMUM_LENGTH = 32;

    private final String value;

    private RememberMeKey(final String value) {
        this.value = value;
    }

    public static RememberMeKey fromConfiguredValue(final String configuredValue) {
        final String normalized = configuredValue == null ? "" : configuredValue.trim();
        final String lowerCase = normalized.toLowerCase(Locale.ROOT);
        if (normalized.isBlank()
                || normalized.length() < MINIMUM_LENGTH
                || normalized.startsWith("${")
                || (normalized.startsWith("<") && normalized.endsWith(">"))
                || lowerCase.contains("change-me")
                || lowerCase.contains("generate")
                || lowerCase.contains("placeholder")
                || lowerCase.contains("example")) {
            throw new IllegalArgumentException(
                    "security.rememberMe.key must be an external secret with at least 32"
                            + " non-placeholder characters");
        }
        return new RememberMeKey(normalized);
    }

    public String value() {
        return value;
    }
}
