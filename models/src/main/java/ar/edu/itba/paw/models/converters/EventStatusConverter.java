package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class EventStatusConverter extends PersistableEnumConverter<EventStatus> {

    public EventStatusConverter() {
        super(EventStatus.class);
    }
}
