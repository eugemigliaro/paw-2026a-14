package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import org.springframework.core.convert.converter.Converter;

public class StringToPlayerReviewReactionConverter
        implements Converter<String, PlayerReviewReaction> {

    @Override
    public PlayerReviewReaction convert(final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(PlayerReviewReaction.class, source.trim()).orElse(null);
    }
}
