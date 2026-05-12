package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.UserBan;
import java.time.Instant;
import java.util.Optional;

public interface UserBanDao {

    UserBan createBan(ModerationReport moderationReport, Instant bannedUntil);

    Optional<UserBan> findById(Long banId);

    Optional<UserBan> findLatestBanForUser(Long userId);

    Optional<UserBan> findActiveBanForUser(Long userId, Instant now);

    void upliftBan(Long banId);
}
