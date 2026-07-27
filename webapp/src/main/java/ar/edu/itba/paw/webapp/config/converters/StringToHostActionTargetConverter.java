package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.webapp.controller.HostActionTarget;
import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;

public class StringToHostActionTargetConverter implements Converter<String, HostActionTarget> {

    @Override
    public HostActionTarget convert(final String source) {
        if (source == null) {
            return null;
        }

        final String trimmed = source.trim();

        return Arrays.stream(HostActionTarget.values())
                .filter(a -> a.getCode().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }
}
