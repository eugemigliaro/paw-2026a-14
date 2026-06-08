package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.RecurrenceFrequency;
import org.springframework.core.convert.converter.Converter;

public class StringToRecurrenceFrequencyConverter
        implements Converter<String, RecurrenceFrequency> {

    @Override
    public RecurrenceFrequency convert(final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(RecurrenceFrequency.class, source.trim()).orElse(null);
    }
}
