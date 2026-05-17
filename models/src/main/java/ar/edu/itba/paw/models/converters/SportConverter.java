package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.Sport;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class SportConverter extends PersistableEnumConverter<Sport> {

    public SportConverter() {
        super(Sport.class);
    }
}
