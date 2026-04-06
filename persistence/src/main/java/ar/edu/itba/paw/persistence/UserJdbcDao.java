package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class UserJdbcDao implements UserDao {

    private static final RowMapper<User> USER_ROW_MAPPER =
            (ResultSet rs, int rowNum) ->
                    new User(rs.getLong("id"), rs.getString("email"), rs.getString("username"));

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public UserJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("users")
                        .usingGeneratedKeyColumns("id");
    }

    @Override
    public User createUser(final String email, final String username) {
        final Map<String, Object> values = new HashMap<>();
        values.put("email", email);
        values.put("username", username);
        values.put("created_at", new java.sql.Timestamp(System.currentTimeMillis()));
        values.put("updated_at", new java.sql.Timestamp(System.currentTimeMillis()));

        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new User(id.longValue(), email, username);
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return jdbcTemplate
                .query("SELECT * FROM users WHERE email = ?", USER_ROW_MAPPER, email)
                .stream()
                .findAny();
    }

    @Override
    public Optional<User> findById(final Long id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", USER_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        return jdbcTemplate
                .query("SELECT * FROM users WHERE username = ?", USER_ROW_MAPPER, username)
                .stream()
                .findAny();
    }
}
