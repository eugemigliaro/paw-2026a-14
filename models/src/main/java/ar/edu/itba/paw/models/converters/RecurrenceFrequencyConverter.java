package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.RecurrenceFrequency;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class RecurrenceFrequencyConverter extends PersistableEnumConverter<RecurrenceFrequency> {

    public RecurrenceFrequencyConverter() {
        super(RecurrenceFrequency.class);
    }
}
