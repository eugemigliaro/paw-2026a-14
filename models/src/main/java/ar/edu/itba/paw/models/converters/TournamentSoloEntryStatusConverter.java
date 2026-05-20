package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class TournamentSoloEntryStatusConverter
        extends PersistableEnumConverter<TournamentSoloEntryStatus> {

    public TournamentSoloEntryStatusConverter() {
        super(TournamentSoloEntryStatus.class);
    }
}
