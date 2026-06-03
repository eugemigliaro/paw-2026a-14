package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToPlayerReviewFilterConverter implements Converter<String, PlayerReviewFilter> {

    @Override
    public PlayerReviewFilter convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return PlayerReviewFilter.fromQueryValue(source.trim()).orElse(null);
    }
}
