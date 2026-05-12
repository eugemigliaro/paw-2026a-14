package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.ReportResolution;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ReportResolutionConverter extends PersistableEnumConverter<ReportResolution> {

    public ReportResolutionConverter() {
        super(ReportResolution.class);
    }
}
