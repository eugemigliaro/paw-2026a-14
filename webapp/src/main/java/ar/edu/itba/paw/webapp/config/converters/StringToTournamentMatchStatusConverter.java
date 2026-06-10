package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import org.springframework.core.convert.converter.Converter;

public class StringToTournamentMatchStatusConverter
        implements Converter<String, TournamentMatchStatus> {

    @Override
    public TournamentMatchStatus convert(final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(TournamentMatchStatus.class, source.trim()).orElse(null);
    }
}
