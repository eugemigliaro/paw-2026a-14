package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ForgotPasswordForm {

    @NotBlank(message = "{ForgotPasswordForm.email.NotBlank}")
    @Email(message = "{ForgotPasswordForm.email.Email}")
    @Size(max = 255, message = "{ForgotPasswordForm.email.Size}")
    private String email = "";

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }
}
