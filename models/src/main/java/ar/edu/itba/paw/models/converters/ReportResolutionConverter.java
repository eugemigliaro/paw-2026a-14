package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.ReportResolution;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ReportResolutionConverter implements AttributeConverter<ReportResolution, String> {

    @Override
    public String convertToDatabaseColumn(final ReportResolution attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public ReportResolution convertToEntityAttribute(final String dbData) {
        return dbData == null ? null : ReportResolution.fromDbValue(dbData).orElseThrow();
    }
}
