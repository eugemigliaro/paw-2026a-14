package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.ReportTargetType;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ReportTargetTypeConverter extends PersistableEnumConverter<ReportTargetType> {

    public ReportTargetTypeConverter() {
        super(ReportTargetType.class);
    }
}
