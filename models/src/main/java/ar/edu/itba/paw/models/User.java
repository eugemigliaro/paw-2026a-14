package ar.edu.itba.paw.models;

public class User {

    private final String email;
    private final String password;
    private final String username;

    public User(final String email, final String password, final String username) {
        this.email = email;
        this.password = password;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "User{"
                + "email='"
                + email
                + '\''
                + ", password='"
                + password
                + '\''
                + ", username='"
                + username
                + '\''
                + '}';
    }
}
