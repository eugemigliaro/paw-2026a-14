package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.ReportStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

public class StringToReportStatusConverter implements Converter<String, ReportStatus> {

    @Override
    public ReportStatus convert(@Nullable final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(ReportStatus.class, source.trim()).orElse(null);
    }
}
