package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.TournamentStatus;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class TournamentStatusConverter extends PersistableEnumConverter<TournamentStatus> {

    public TournamentStatusConverter() {
        super(TournamentStatus.class);
    }
}
