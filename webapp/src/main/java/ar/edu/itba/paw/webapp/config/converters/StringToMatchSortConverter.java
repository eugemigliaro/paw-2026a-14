package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.query.EventSort;
import org.springframework.core.convert.converter.Converter;

public class StringToMatchSortConverter implements Converter<String, EventSort> {

    @Override
    public EventSort convert(final String source) {
        if (source == null) {
            return null;
        }

        return EventSort.fromQueryValue(source.trim()).orElse(null);
    }
}
