package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BanAppealDecision;
import ar.edu.itba.paw.models.UserBan;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserBanDao {

    UserBan createBan(Long userId, Long bannedByUserId, Instant bannedUntil, String reason);

    Optional<UserBan> findById(Long banId);

    Optional<UserBan> findLatestBanForUser(Long userId);

    Optional<UserBan> findActiveBanForUser(Long userId, Instant now);

    boolean appealBan(Long banId, String appealReason, Instant appealedAt);

    boolean resolveAppeal(
            Long banId, Long adminUserId, BanAppealDecision decision, Instant resolvedAt);

    List<UserBan> findBansForUser(Long userId);
}
