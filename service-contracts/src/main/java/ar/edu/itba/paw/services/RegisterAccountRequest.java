package ar.edu.itba.paw.services;

public class RegisterAccountRequest {

    private final String email;
    private final String username;
    private final String password;

    public RegisterAccountRequest(
            final String email, final String username, final String password) {
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
