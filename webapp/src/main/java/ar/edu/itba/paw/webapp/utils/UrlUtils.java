package ar.edu.itba.paw.webapp.utils;

import java.util.Locale;

public final class UrlUtils {

    private UrlUtils() {
    }

    public static String withLang(final String path, final Locale locale) {
        if (locale == null)
            return path;

        final String lang = locale.getLanguage();

        return path.contains("?")
                ? path + "&lang=" + lang
                : path + "?lang=" + lang;
    }
}