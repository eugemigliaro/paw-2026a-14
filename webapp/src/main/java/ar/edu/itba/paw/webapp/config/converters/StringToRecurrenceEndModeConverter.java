package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.RecurrenceEndMode;
import org.springframework.core.convert.converter.Converter;

public class StringToRecurrenceEndModeConverter implements Converter<String, RecurrenceEndMode> {

    @Override
    public RecurrenceEndMode convert(final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(RecurrenceEndMode.class, source.trim()).orElse(null);
    }
}
