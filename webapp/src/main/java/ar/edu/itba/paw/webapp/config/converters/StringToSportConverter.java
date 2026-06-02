package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.Sport;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToSportConverter implements Converter<String, Sport> {

    @Override
    public Sport convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(Sport.class, source.trim()).orElse(null);
    }
}
