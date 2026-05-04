package ar.edu.itba.paw.models;

import java.util.Locale;

public final class UserLanguages {

    public static final String ENGLISH = "en";
    public static final String SPANISH = "es";
    public static final String DEFAULT_LANGUAGE = ENGLISH;

    private UserLanguages() {}

    public static String normalizeLanguage(final String language) {
        if (language == null || language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }

        final String normalized = primaryLanguage(language);
        if (SPANISH.equals(normalized)) {
            return SPANISH;
        }
        return ENGLISH;
    }

    public static boolean isSupportedLanguage(final String language) {
        final String normalized = primaryLanguage(language);
        return ENGLISH.equals(normalized) || SPANISH.equals(normalized);
    }

    public static String fromLocale(final Locale locale) {
        if (locale == null) {
            return DEFAULT_LANGUAGE;
        }
        return normalizeLanguage(locale.getLanguage());
    }

    public static Locale toLocale(final String language) {
        return Locale.forLanguageTag(normalizeLanguage(language));
    }

    private static String primaryLanguage(final String language) {
        if (language == null || language.isBlank()) {
            return "";
        }
        final String normalized = language.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        final int separator = normalized.indexOf('-');
        return separator < 0 ? normalized : normalized.substring(0, separator);
    }
}
