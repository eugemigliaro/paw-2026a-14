package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ParticipantStatusConverter extends PersistableEnumConverter<ParticipantStatus> {

    public ParticipantStatusConverter() {
        super(ParticipantStatus.class);
    }
}
