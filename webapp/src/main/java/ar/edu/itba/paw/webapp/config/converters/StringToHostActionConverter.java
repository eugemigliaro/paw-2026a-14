package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.webapp.controller.HostAction;
import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;

public class StringToHostActionConverter implements Converter<String, HostAction> {

    @Override
    public HostAction convert(final String source) {
        if (source == null) {
            return null;
        }

        final String trimmed = source.trim();

        return Arrays.stream(HostAction.values())
                .filter(a -> a.getCode().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }
}
