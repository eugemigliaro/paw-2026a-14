package ar.edu.itba.paw.models;

public class User {

    private final Long id;
    private final String email;
    private final String password;
    private final String username;

    public User(final Long id, final String email, final String password, final String username) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.username = username;
    }

    public Long getId() {
        return id;
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
                + ", id='"
                + id
                + '\''
                + '}';
    }
}
