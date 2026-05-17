package ar.edu.itba.paw.models.converters;

import ar.edu.itba.paw.models.types.PersistableEnumConverter;
import ar.edu.itba.paw.models.types.UserRole;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class UserRoleConverter extends PersistableEnumConverter<UserRole> {

    public UserRoleConverter() {
        super(UserRole.class);
    }
}
