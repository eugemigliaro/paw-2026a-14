package ar.edu.itba.paw.webapp.config.converters;

import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.PersistableEnum;
import org.springframework.core.convert.converter.Converter;

public class StringToAppealDecisionConverter implements Converter<String, AppealDecision> {

    @Override
    public AppealDecision convert(final String source) {
        if (source == null) {
            return null;
        }

        return PersistableEnum.fromDbValue(AppealDecision.class, source.trim()).orElse(null);
    }
}
