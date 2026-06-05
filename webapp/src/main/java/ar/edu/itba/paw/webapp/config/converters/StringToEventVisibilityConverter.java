package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.PersistableEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToEventVisibilityConverter implements Converter<String, EventVisibility> {

    @Override
    public EventVisibility convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(EventVisibility.class, source.trim()).orElse(null);
    }
}
