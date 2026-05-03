package ar.edu.itba.paw.webapp.utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EnumFilterUtils {

    private EnumFilterUtils() {}

    public static <T> List<T> parseEnumFilters(
            final List<String> rawValues, final Function<String, Optional<T>> parser) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        final Set<T> parsed =
                rawValues.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(parser)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(parsed);
    }
}
