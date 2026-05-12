package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class EventVisibilityConverter extends PersistableEnumConverter<EventVisibility> {

    public EventVisibilityConverter() {
        super(EventVisibility.class);
    }
}
