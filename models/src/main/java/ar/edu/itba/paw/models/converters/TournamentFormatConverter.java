package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.TournamentFormat;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class TournamentFormatConverter extends PersistableEnumConverter<TournamentFormat> {

    public TournamentFormatConverter() {
        super(TournamentFormat.class);
    }
}
