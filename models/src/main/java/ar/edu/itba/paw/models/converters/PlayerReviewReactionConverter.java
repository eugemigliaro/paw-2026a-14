package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class PlayerReviewReactionConverter extends PersistableEnumConverter<PlayerReviewReaction> {

    public PlayerReviewReactionConverter() {
        super(PlayerReviewReaction.class);
    }
}
