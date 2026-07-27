package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.webapp.controller.InviteStatus;
import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;

public class StringToInviteStatusConverter implements Converter<String, InviteStatus> {

    @Override
    public InviteStatus convert(final String source) {
        if (source == null) {
            return null;
        }

        final String trimmed = source.trim();

        return Arrays.stream(InviteStatus.values())
                .filter(s -> s.getCode().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }
}
