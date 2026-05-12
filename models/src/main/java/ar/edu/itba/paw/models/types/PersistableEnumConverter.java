package ar.edu.itba.paw.models.types;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public abstract class PersistableEnumConverter<T extends Enum<T> & PersistableEnum>
        implements AttributeConverter<T, String> {

    private final Class<T> enumType;

    protected PersistableEnumConverter(final Class<T> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(final T attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public T convertToEntityAttribute(final String dbData) {
        return dbData == null ? null : PersistableEnum.fromDbValue(enumType, dbData).orElseThrow();
    }
}
