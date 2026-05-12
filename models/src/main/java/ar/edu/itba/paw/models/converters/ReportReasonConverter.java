package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.ReportReason;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ReportReasonConverter implements AttributeConverter<ReportReason, String> {
    @Override
    public String convertToDatabaseColumn(final ReportReason attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public ReportReason convertToEntityAttribute(final String dbData) {
        return dbData == null ? null : ReportReason.fromDbValue(dbData).orElseThrow();
    }
}
