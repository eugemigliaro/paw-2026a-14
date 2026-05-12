package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.ReportStatus;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ReportStatusConverter implements AttributeConverter<ReportStatus, String> {
    @Override
    public String convertToDatabaseColumn(final ReportStatus attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public ReportStatus convertToEntityAttribute(final String dbData) {
        return dbData == null ? null : ReportStatus.fromDbValue(dbData).orElseThrow();
    }
}
