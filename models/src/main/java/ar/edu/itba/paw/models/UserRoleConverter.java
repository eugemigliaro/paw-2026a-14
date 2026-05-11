package ar.edu.itba.paw.models;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(final UserRole attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public UserRole convertToEntityAttribute(final String dbData) {
        return dbData == null ? null : UserRole.fromDbValue(dbData).orElseThrow();
    }
}
