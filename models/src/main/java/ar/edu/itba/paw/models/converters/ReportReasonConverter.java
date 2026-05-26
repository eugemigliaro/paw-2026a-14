package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.ReportReason;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ReportReasonConverter extends PersistableEnumConverter<ReportReason> {

    public ReportReasonConverter() {
        super(ReportReason.class);
    }
}
