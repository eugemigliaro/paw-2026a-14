package ar.edu.itba.paw.models.types;

import java.util.Arrays;
import java.util.Optional;

public interface PersistableEnum {

    String getDbValue();

    default String getValue() {
        return getDbValue();
    }

    static <E extends Enum<E> & PersistableEnum> Optional<E> fromDbValue(
            final Class<E> enumType, final String dbValue) {
        if (dbValue == null) {
            return Optional.empty();
        }

        final String normalizedDbValue = dbValue.trim();
        return Arrays.stream(enumType.getEnumConstants())
                .filter(value -> value.getDbValue().equalsIgnoreCase(normalizedDbValue))
                .findFirst();
    }
}
