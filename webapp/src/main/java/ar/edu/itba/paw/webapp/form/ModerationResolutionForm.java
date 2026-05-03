package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Size;

public class ModerationResolutionForm {

    @Size(max = 4000, message = "{Size.moderationResolutionForm.resolutionDetails}")
    private String resolutionDetails;

    public String getResolutionDetails() {
        return resolutionDetails;
    }

    public void setResolutionDetails(String resolutionDetails) {
        this.resolutionDetails = resolutionDetails;
    }
}
