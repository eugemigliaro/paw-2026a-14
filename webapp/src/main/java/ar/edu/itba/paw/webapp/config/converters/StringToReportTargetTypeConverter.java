package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.ReportTargetType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToReportTargetTypeConverter implements Converter<String, ReportTargetType> {

    @Override
    public ReportTargetType convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(ReportTargetType.class, source.trim()).orElse(null);
    }
}
