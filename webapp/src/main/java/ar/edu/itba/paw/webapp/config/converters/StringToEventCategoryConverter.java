package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.query.EventCategory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToEventCategoryConverter implements Converter<String, EventCategory> {

    @Override
    public EventCategory convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return EventCategory.fromQueryValue(source.trim()).orElse(null);
    }
}
