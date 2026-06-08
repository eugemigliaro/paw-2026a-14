package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.query.EventFilter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToEventFilterConverter implements Converter<String, EventFilter> {

    @Override
    public EventFilter convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return EventFilter.fromQueryValue(source.trim()).orElse(null);
    }
}
