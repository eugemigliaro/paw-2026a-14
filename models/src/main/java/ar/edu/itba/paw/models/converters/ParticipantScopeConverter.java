package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.ParticipantScope;
import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ParticipantScopeConverter extends PersistableEnumConverter<ParticipantScope> {

    public ParticipantScopeConverter() {
        super(ParticipantScope.class);
    }
}
