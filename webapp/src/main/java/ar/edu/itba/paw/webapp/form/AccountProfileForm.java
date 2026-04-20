package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AccountProfileForm {

    @NotBlank(message = "{AccountProfileForm.username.NotBlank}")
    @Pattern(regexp = "^[a-z0-9_]{3,50}$", message = "{AccountProfileForm.username.Pattern}")
    private String username;

    @NotBlank(message = "{AccountProfileForm.name.NotBlank}")
    @Size(max = 150, message = "{AccountProfileForm.name.Size}")
    private String name;

    @NotBlank(message = "{AccountProfileForm.lastName.NotBlank}")
    @Size(max = 150, message = "{AccountProfileForm.lastName.Size}")
    private String lastName;

    @Size(max = 50, message = "{AccountProfileForm.phone.Size}")
    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,50}$", message = "{AccountProfileForm.phone.Pattern}")
    private String phone;

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
}
