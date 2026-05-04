package ar.edu.itba.paw.models;

import java.time.Instant;

public class UserAccount {

    private final Long id;
    private final String email;
    private final String username;
    private final String name;
    private final String lastName;
    private final String phone;
    private final Long profileImageId;
    private final String passwordHash;
    private final UserRole role;
    private final Instant emailVerifiedAt;
    private final String preferredLanguage;

    public UserAccount(
            final Long id,
            final String email,
            final String username,
            final String passwordHash,
            final UserRole role,
            final Instant emailVerifiedAt) {
        this(
                id,
                email,
                username,
                null,
                null,
                null,
                null,
                passwordHash,
                role,
                emailVerifiedAt,
                UserLanguages.DEFAULT_LANGUAGE);
    }

    public UserAccount(
            final Long id,
            final String email,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final Long profileImageId,
            final String passwordHash,
            final UserRole role,
            final Instant emailVerifiedAt) {
        this(
                id,
                email,
                username,
                name,
                lastName,
                phone,
                profileImageId,
                passwordHash,
                role,
                emailVerifiedAt,
                UserLanguages.DEFAULT_LANGUAGE);
    }

    public UserAccount(
            final Long id,
            final String email,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final Long profileImageId,
            final String passwordHash,
            final UserRole role,
            final Instant emailVerifiedAt,
            final String preferredLanguage) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.profileImageId = profileImageId;
        this.passwordHash = passwordHash;
        this.role = role;
        this.emailVerifiedAt = emailVerifiedAt;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    @Override
    public String toString() {
        return "UserAccount{"
                + "id="
                + id
                + ", username='"
                + username
                + '\''
                + ", role="
                + role
                + ", profileImageId="
                + profileImageId
                + ", emailVerified="
                + isEmailVerified()
                + ", emailVerifiedAt="
                + emailVerifiedAt
                + ", hasEmail="
                + (email != null && !email.isBlank())
                + ", hasName="
                + (name != null && !name.isBlank())
                + ", hasLastName="
                + (lastName != null && !lastName.isBlank())
                + ", hasPhone="
                + (phone != null && !phone.isBlank())
                + ", hasPassword="
                + hasPassword()
                + ", preferredLanguage='"
                + preferredLanguage
                + '\''
                + '}';
    }
}
