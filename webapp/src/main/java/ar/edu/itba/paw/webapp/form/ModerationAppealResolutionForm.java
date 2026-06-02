package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.types.AppealDecision;
import javax.validation.constraints.NotNull;

public class ModerationAppealResolutionForm {

    @NotNull(message = "{NotNull.moderationAppealResolutionForm.appealDecision}")
    private AppealDecision appealDecision = AppealDecision.UPHELD;

    public AppealDecision getAppealDecision() {
        return appealDecision;
    }

    public void setAppealDecision(final AppealDecision appealDecision) {
        this.appealDecision = appealDecision;
    }
}
