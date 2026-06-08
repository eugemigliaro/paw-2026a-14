package ar.edu.itba.paw.webapp.config.converters;

import java.time.ZoneId;
import org.springframework.core.convert.converter.Converter;

public class StringToZoneIdConverter implements Converter<String, ZoneId> {

    @Override
    public ZoneId convert(final String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        try {
            return ZoneId.of(source.trim());
        } catch (final Exception ignored) {
            return null;
        }
    }
}
