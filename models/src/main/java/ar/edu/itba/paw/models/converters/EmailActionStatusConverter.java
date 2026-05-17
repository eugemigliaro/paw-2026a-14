package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.EmailActionStatus;
import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class EmailActionStatusConverter extends PersistableEnumConverter<EmailActionStatus> {

    public EmailActionStatusConverter() {
        super(EmailActionStatus.class);
    }
}
