package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.ReportReason;
import org.springframework.core.convert.converter.Converter;

public class StringToReportReasonConverter implements Converter<String, ReportReason> {

    @Override
    public ReportReason convert(final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(ReportReason.class, source.trim()).orElse(null);
    }
}
