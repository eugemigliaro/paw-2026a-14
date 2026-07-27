package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.webapp.controller.JoinStatus;
import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;

public class StringToJoinStatusConverter implements Converter<String, JoinStatus> {

    @Override
    public JoinStatus convert(final String source) {
        if (source == null) {
            return null;
        }

        final String trimmed = source.trim();

        return Arrays.stream(JoinStatus.values())
                .filter(s -> s.getCode().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }
}
