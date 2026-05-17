package ar.edu.itba.paw.models;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class User {

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_id")
    private ImageMetadata profileImageMetadata;

    @Column(name = "preferred_language", length = 5, nullable = false)
    private String preferredLanguage;

    User() {}

    public User(
            final Long id,
            final String email,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final ImageMetadata profileImageMetadata,
            final String preferredLanguage) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.profileImageMetadata = profileImageMetadata;
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

    public ImageMetadata getProfileImageMetadata() {
        return profileImageMetadata;
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
                + (profileImageMetadata == null ? null : profileImageMetadata.getId())
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof User)) {
            return false;
        }

        User user = (User) o;

        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
