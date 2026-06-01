package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.query.MatchSort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToMatchSortConverter implements Converter<String, MatchSort> {

    @Override
    public MatchSort convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return MatchSort.fromQueryValue(source.trim()).orElse(null);
    }
}
