package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class TournamentMatchStatusConverter
        extends PersistableEnumConverter<TournamentMatchStatus> {

    public TournamentMatchStatusConverter() {
        super(TournamentMatchStatus.class);
    }
}
