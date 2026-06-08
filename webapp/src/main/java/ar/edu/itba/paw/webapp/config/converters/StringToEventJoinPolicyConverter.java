package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.PersistableEnum;
import org.springframework.core.convert.converter.Converter;

public class StringToEventJoinPolicyConverter implements Converter<String, EventJoinPolicy> {

    @Override
    public EventJoinPolicy convert(final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(EventJoinPolicy.class, source.trim()).orElse(null);
    }
}
