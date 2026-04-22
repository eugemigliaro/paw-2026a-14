package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RegisterForm {

    @NotBlank(message = "{RegisterForm.email.NotBlank}")
    @Email(message = "{RegisterForm.email.Email}")
    @Size(max = 255, message = "{RegisterForm.email.Size}")
    private String email = "";

    @NotBlank(message = "{RegisterForm.username.NotBlank}")
    @Pattern(regexp = "^[a-z0-9_]{3,50}$", message = "{RegisterForm.username.Pattern}")
    private String username = "";

    @NotBlank(message = "{RegisterForm.name.NotBlank}")
    @Size(max = 150, message = "{RegisterForm.name.Size}")
    private String name = "";

    @NotBlank(message = "{RegisterForm.lastName.NotBlank}")
    @Size(max = 150, message = "{RegisterForm.lastName.Size}")
    private String lastName = "";

    @Size(max = 50, message = "{RegisterForm.phone.Size}")
    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,50}$", message = "{RegisterForm.phone.Pattern}")
    private String phone = "";

    @NotBlank(message = "{RegisterForm.password.NotBlank}")
    @Size(min = 8, max = 72, message = "{RegisterForm.password.Size}")
    private String password = "";

    @NotBlank(message = "{RegisterForm.confirmPassword.NotBlank}")
    private String confirmPassword = "";

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

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
