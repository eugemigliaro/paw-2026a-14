package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.ReportStatus;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ReportStatusConverter extends PersistableEnumConverter<ReportStatus> {

    public ReportStatusConverter() {
        super(ReportStatus.class);
    }
}
