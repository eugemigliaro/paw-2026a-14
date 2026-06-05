package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.validation.ValidUserEmail;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class InviteForm {
    @NotBlank(message = "{InviteForm.email.NotBlank}")
    @Email(message = "{InviteForm.email.Email}")
    @Size(max = 255, message = "{InviteForm.email.Size}")
    @ValidUserEmail(message = "{validation.user.email.mustExist}", mustExist = true)
    private String email;

    private boolean inviteSeries;

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public boolean isInviteSeries() {
        return inviteSeries;
    }

    public void setInviteSeries(final boolean inviteSeries) {
        this.inviteSeries = inviteSeries;
    }
}
