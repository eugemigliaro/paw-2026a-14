package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.EmailActionType;
import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class EmailActionTypeConverter extends PersistableEnumConverter<EmailActionType> {

    public EmailActionTypeConverter() {
        super(EmailActionType.class);
    }
}
