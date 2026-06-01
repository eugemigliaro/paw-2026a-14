package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToTournamentPairingStrategyConverter
        implements Converter<String, TournamentPairingStrategy> {

    @Override
    public TournamentPairingStrategy convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(TournamentPairingStrategy.class, source.trim())
                .orElse(null);
    }
}
