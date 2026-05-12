package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.AppealDecision;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class AppealDecisionConverter implements AttributeConverter<AppealDecision, String> {

    @Override
    public String convertToDatabaseColumn(final AppealDecision attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public AppealDecision convertToEntityAttribute(final String dbData) {
        return dbData == null ? null : AppealDecision.fromDbValue(dbData).orElseThrow();
    }
}
