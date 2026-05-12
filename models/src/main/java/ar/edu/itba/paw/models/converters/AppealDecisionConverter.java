package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class AppealDecisionConverter extends PersistableEnumConverter<AppealDecision> {

    public AppealDecisionConverter() {
        super(AppealDecision.class);
    }
}
