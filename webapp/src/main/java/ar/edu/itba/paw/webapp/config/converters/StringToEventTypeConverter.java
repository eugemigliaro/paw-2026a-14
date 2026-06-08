package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.models.types.PersistableEnum;
import org.springframework.core.convert.converter.Converter;

public class StringToEventTypeConverter implements Converter<String, EventType> {

    @Override
    public EventType convert(final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(EventType.class, source.trim()).orElse(null);
    }
}
