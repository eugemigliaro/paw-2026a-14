package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class ModerationAppealResolutionForm {

    @NotBlank(message = "{NotBlank.moderationAppealResolutionForm.appealDecision}")
    @Pattern(
            regexp = "upheld|lifted",
            message = "{Pattern.moderationAppealResolutionForm.appealDecision}")
    private String appealDecision = "upheld";

    public String getAppealDecision() {
        return appealDecision;
    }

    public void setAppealDecision(final String appealDecision) {
        this.appealDecision = appealDecision;
    }
}
