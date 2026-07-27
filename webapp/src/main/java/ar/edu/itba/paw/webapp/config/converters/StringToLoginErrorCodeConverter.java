package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.webapp.controller.LoginErrorCode;
import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;

public class StringToLoginErrorCodeConverter implements Converter<String, LoginErrorCode> {

    @Override
    public LoginErrorCode convert(final String source) {
        if (source == null) {
            return null;
        }

        final String trimmed = source.trim();

        return Arrays.stream(LoginErrorCode.values())
                .filter(ec -> ec.getCode().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }
}
