package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.MatchSort;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class MatchFilterQueryUtils {

    private MatchFilterQueryUtils() {}

    public static String normalizeSort(final String sort) {
        if (sort == null || sort.isBlank()) {
            return MatchSort.SOONEST.getQueryValue();
        }

        return MatchSort.fromQueryValue(sort)
                .map(MatchSort::getQueryValue)
                .orElse(MatchSort.SOONEST.getQueryValue());
    }

    public static String normalizeTime(final String time) {
        if (time == null || time.isBlank()) {
            return "all";
        }

        switch (time.toLowerCase(Locale.ROOT)) {
            case "today":
                return "today";
            case "tomorrow":
                return "tomorrow";
            case "week":
                return "week";
            default:
                return "all";
        }
    }

    public static List<String> normalizeCsvValues(final List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }

        final LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (final String rawValue : rawValues) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }

            for (final String part : rawValue.split(",")) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                normalized.add(part.trim().toLowerCase(Locale.ROOT));
            }
        }

        return List.copyOf(normalized);
    }

    public static String encodeCsv(final List<String> values) {
        final List<String> normalized = normalizeCsvValues(values);
        if (normalized.isEmpty()) {
            return null;
        }

        return String.join(",", normalized);
    }

    public static List<String> toggleValue(final List<String> selectedValues, final String value) {
        final LinkedHashSet<String> normalized =
                new LinkedHashSet<>(normalizeCsvValues(selectedValues));
        final String normalizedValue = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        if (normalizedValue.isBlank()) {
            return List.copyOf(normalized);
        }

        if (!normalized.remove(normalizedValue)) {
            normalized.add(normalizedValue);
        }

        return List.copyOf(normalized);
    }
}
