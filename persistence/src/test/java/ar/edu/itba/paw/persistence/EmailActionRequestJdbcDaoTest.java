package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.EmailActionStatus;
import ar.edu.itba.paw.models.EmailActionType;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class EmailActionRequestJdbcDaoTest {

    @Autowired private EmailActionRequestDao emailActionRequestDao;

    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, created_at, updated_at)"
                        + " VALUES (1, 'host', 'host@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }

    @Test
    public void testCreateAndFindByTokenHash() {
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.MATCH_RESERVATION,
                        "player@test.com",
                        1L,
                        "token-hash",
                        "{\"matchId\":10}",
                        Instant.parse("2026-04-06T20:15:00Z"));

        final EmailActionRequest found =
                emailActionRequestDao.findByTokenHash("token-hash").orElseThrow();

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals(created.getId(), found.getId());
        Assertions.assertEquals(EmailActionType.MATCH_RESERVATION, found.getActionType());
        Assertions.assertEquals("player@test.com", found.getEmail());
        Assertions.assertEquals(1L, found.getUserId());
        Assertions.assertEquals(EmailActionStatus.PENDING, found.getStatus());
    }

    @Test
    public void testUpdateStatusMarksRequestAsCompleted() {
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.MATCH_RESERVATION,
                        "player@test.com",
                        null,
                        "completion-hash",
                        "{\"matchId\":10}",
                        Instant.parse("2026-04-06T20:15:00Z"));

        final Instant consumedAt = Instant.parse("2026-04-05T20:15:00Z");
        emailActionRequestDao.updateStatus(
                created.getId(), EmailActionStatus.COMPLETED, 1L, consumedAt);

        final EmailActionRequest updated =
                emailActionRequestDao.findByTokenHash("completion-hash").orElseThrow();

        Assertions.assertEquals(EmailActionStatus.COMPLETED, updated.getStatus());
        Assertions.assertEquals(1L, updated.getUserId());
        Assertions.assertEquals(consumedAt, updated.getConsumedAt());
    }
}
