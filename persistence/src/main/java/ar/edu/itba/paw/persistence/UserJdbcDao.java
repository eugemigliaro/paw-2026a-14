package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class UserJdbcDao implements UserDao {

    @NonNull
    private static final RowMapper<User> USER_ROW_MAPPER =
            (ResultSet rs, int rowNum) ->
                    new User(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("username"),
                            rs.getString("name"),
                            rs.getString("last_name"),
                            rs.getString("phone"),
                            rs.getObject("profile_image_id") == null
                                    ? null
                                    : rs.getLong("profile_image_id"));

    @NonNull
    private static final RowMapper<UserAccount> USER_ACCOUNT_ROW_MAPPER =
            (ResultSet rs, int rowNum) -> {
                final Timestamp emailVerifiedAt = rs.getTimestamp("email_verified_at");
                return new UserAccount(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("username"),
                        rs.getString("name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getObject("profile_image_id") == null
                                ? null
                                : rs.getLong("profile_image_id"),
                        rs.getString("password_hash"),
                        UserRole.fromDbValue(rs.getString("role")).orElse(UserRole.USER),
                        emailVerifiedAt == null ? null : emailVerifiedAt.toInstant());
            };

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public UserJdbcDao(@NonNull final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("users")
                        .usingGeneratedKeyColumns("id");
    }

    @Override
    public User createUser(final String email, final String username) {
        final Instant now = Instant.now();
        final Map<String, Object> values = new HashMap<>();
        values.put("email", email);
        values.put("username", username);
        values.put("role", UserRole.USER.getDbValue());
        values.put("email_verified_at", Timestamp.from(now));
        values.put("created_at", Timestamp.from(now));
        values.put("updated_at", Timestamp.from(now));

        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new User(id.longValue(), email, username);
    }

    @Override
    public UserAccount createAccount(
            final String email,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final String passwordHash,
            final UserRole role,
            final Instant emailVerifiedAt) {
        final Instant now = Instant.now();
        final Map<String, Object> values = new HashMap<>();
        values.put("email", email);
        values.put("username", username);
        values.put("name", name);
        values.put("last_name", lastName);
        values.put("phone", phone);
        values.put("password_hash", passwordHash);
        values.put("role", role.getDbValue());
        values.put(
                "email_verified_at",
                emailVerifiedAt == null ? null : Timestamp.from(emailVerifiedAt));
        values.put("created_at", Timestamp.from(now));
        values.put("updated_at", Timestamp.from(now));

        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new UserAccount(
                id.longValue(),
                email,
                username,
                name,
                lastName,
                phone,
                null,
                passwordHash,
                role,
                emailVerifiedAt);
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return jdbcTemplate
                .query("SELECT * FROM users WHERE email = ?", USER_ROW_MAPPER, email)
                .stream()
                .findAny();
    }

    @Override
    public Optional<UserAccount> findAccountByEmail(final String email) {
        return jdbcTemplate
                .query("SELECT * FROM users WHERE email = ?", USER_ACCOUNT_ROW_MAPPER, email)
                .stream()
                .findAny();
    }

    @Override
    public Optional<User> findById(final Long id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", USER_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public List<User> findByIds(final Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return namedParameterJdbcTemplate.query(
                "SELECT * FROM users WHERE id IN (:ids)", Map.of("ids", ids), USER_ROW_MAPPER);
    }

    @Override
    public Optional<UserAccount> findAccountById(final Long id) {
        return jdbcTemplate
                .query("SELECT * FROM users WHERE id = ?", USER_ACCOUNT_ROW_MAPPER, id)
                .stream()
                .findAny();
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        return jdbcTemplate
                .query("SELECT * FROM users WHERE username = ?", USER_ROW_MAPPER, username)
                .stream()
                .findAny();
    }

    @Override
    public void updateProfile(
            final Long id,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final Long profileImageId) {
        jdbcTemplate.update(
                "UPDATE users "
                        + "SET username = ?, name = ?, last_name = ?, phone = ?, profile_image_id = ?, updated_at = ? "
                        + "WHERE id = ?",
                username,
                name,
                lastName,
                phone,
                profileImageId,
                Timestamp.from(Instant.now()),
                id);
    }

    @Override
    public void updateProfileImage(final Long id, final Long profileImageId) {
        jdbcTemplate.update(
                "UPDATE users SET profile_image_id = ?, updated_at = ? WHERE id = ?",
                profileImageId,
                Timestamp.from(Instant.now()),
                id);
    }

    @Override
    public void updatePasswordHash(final Long id, final String passwordHash) {
        jdbcTemplate.update(
                "UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?",
                passwordHash,
                Timestamp.from(Instant.now()),
                id);
    }

    @Override
    public void markEmailVerified(final Long id, final Instant emailVerifiedAt) {
        jdbcTemplate.update(
                "UPDATE users SET email_verified_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.from(emailVerifiedAt),
                Timestamp.from(Instant.now()),
                id);
    }
}
