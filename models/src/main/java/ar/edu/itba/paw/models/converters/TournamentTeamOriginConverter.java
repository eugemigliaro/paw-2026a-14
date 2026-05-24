package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class TournamentTeamOriginConverter extends PersistableEnumConverter<TournamentTeamOrigin> {

    public TournamentTeamOriginConverter() {
        super(TournamentTeamOrigin.class);
    }
}
