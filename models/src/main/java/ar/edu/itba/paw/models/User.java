package ar.edu.itba.paw.models;

public class User {

    private final Long id;
    private final String email;
    private final String username;
    private final String name;
    private final String lastName;
    private final String phone;
    private final Long profileImageId;
    private final String preferredLanguage;

    public User(final Long id, final String email, final String username) {
        this(id, email, username, null, null, null, null, UserLanguages.DEFAULT_LANGUAGE);
    }

    public User(
            final Long id,
            final String email,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final Long profileImageId) {
        this(
                id,
                email,
                username,
                name,
                lastName,
                phone,
                profileImageId,
                UserLanguages.DEFAULT_LANGUAGE);
    }

    public User(
            final Long id,
            final String email,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final Long profileImageId,
            final String preferredLanguage) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.profileImageId = profileImageId;
        this.preferredLanguage = UserLanguages.normalizeLanguage(preferredLanguage);
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

    public Long getProfileImageId() {
        return profileImageId;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    @Override
    public String toString() {
        return "User{"
                + "id="
                + id
                + ", username='"
                + username
                + '\''
                + ", profileImageId="
                + profileImageId
                + ", hasEmail="
                + (email != null && !email.isBlank())
                + ", hasName="
                + (name != null && !name.isBlank())
                + ", hasLastName="
                + (lastName != null && !lastName.isBlank())
                + ", hasPhone="
                + (phone != null && !phone.isBlank())
                + ", preferredLanguage='"
                + preferredLanguage
                + '\''
                + '}';
    }
}
