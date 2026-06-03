package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.PersistableEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToEventStatusConverter implements Converter<String, EventStatus> {

    @Override
    public EventStatus convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(EventStatus.class, source.trim()).orElse(null);
    }
}
