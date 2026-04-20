package ar.edu.itba.paw.models;

public class User {

    private final Long id;
    private final String email;
    private final String username;
    private final String name;
    private final String lastName;
    private final String phone;

    public User(final Long id, final String email, final String username) {
        this(id, email, username, null, null, null);
    }

    public User(
            final Long id,
            final String email,
            final String username,
            final String name,
            final String lastName,
            final String phone) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
    }

    public Long getId() {
        return id;
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

    @Override
    public String toString() {
        return "User{"
                + "email='"
                + email
                + '\''
                + ", username='"
                + username
                + '\''
                + ", name='"
                + name
                + '\''
                + ", lastName='"
                + lastName
                + '\''
                + ", phone='"
                + phone
                + '\''
                + ", id='"
                + id
                + '\''
                + '}';
    }
}
