package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class EventJoinPolicyConverter extends PersistableEnumConverter<EventJoinPolicy> {

    public EventJoinPolicyConverter() {
        super(EventJoinPolicy.class);
    }
}
