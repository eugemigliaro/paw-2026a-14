package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class InviteForm {

    @NotBlank
    @Email
    @Size(max = 255)
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
