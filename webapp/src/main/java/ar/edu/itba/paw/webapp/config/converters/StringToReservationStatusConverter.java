package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.webapp.controller.ReservationStatus;
import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;

public class StringToReservationStatusConverter implements Converter<String, ReservationStatus> {

    @Override
    public ReservationStatus convert(final String source) {
        if (source == null) {
            return null;
        }

        final String trimmed = source.trim();

        return Arrays.stream(ReservationStatus.values())
                .filter(s -> s.getCode().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }
}
