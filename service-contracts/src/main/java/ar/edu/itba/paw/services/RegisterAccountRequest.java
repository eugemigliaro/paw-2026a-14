package ar.edu.itba.paw.services;

public class RegisterAccountRequest {

    private final String email;
    private final String username;
    private final String name;
    private final String lastName;
    private final String phone;
    private final String password;

    public RegisterAccountRequest(
            final String email,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final String password) {
        this.email = email;
        this.username = username;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }
}
