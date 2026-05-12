package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.ReportTargetType;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ReportTargetTypeConverter implements AttributeConverter<ReportTargetType, String> {
    @Override
    public String convertToDatabaseColumn(final ReportTargetType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public ReportTargetType convertToEntityAttribute(final String dbData) {
        return dbData == null ? null : ReportTargetType.fromDbValue(dbData).orElseThrow();
    }
}
