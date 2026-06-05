package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ResetPasswordForm {

    @NotBlank(message = "{ResetPasswordForm.password.NotBlank}")
    @Size(min = 8, max = 72, message = "{ResetPasswordForm.password.Size}")
    private String password = "";

    @NotBlank(message = "{ResetPasswordForm.confirmPassword.NotBlank}")
    private String confirmPassword = "";

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(final String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
