package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.UserRoleConverter;
import ar.edu.itba.paw.models.types.UserRole;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_userid_seq")
    @SequenceGenerator(
            sequenceName = "users_userid_seq",
            name = "users_userid_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "last_name", length = 150)
    private String lastName;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "profile_image_id")
    private Long profileImageId;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "role", length = 30, nullable = false)
    @Convert(converter = UserRoleConverter.class)
    private UserRole role;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "preferred_language", length = 5, nullable = false)
    private String preferredLanguage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default no-arg constructor for JPA
    UserAccount() {}

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
                preferredLanguage,
                null,
                null);
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
            final String preferredLanguage,
            final Instant createdAt,
            final Instant updatedAt) {
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public void setProfileImageId(final Long profileImageId) {
        this.profileImageId = profileImageId;
    }

    public void setPasswordHash(final String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setEmailVerifiedAt(final Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public void setPreferredLanguage(final String preferredLanguage) {
        this.preferredLanguage = UserLanguages.normalizeLanguage(preferredLanguage);
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
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
