package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class TournamentPairingStrategyConverter
        extends PersistableEnumConverter<TournamentPairingStrategy> {

    public TournamentPairingStrategyConverter() {
        super(TournamentPairingStrategy.class);
    }
}
