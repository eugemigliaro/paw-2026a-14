package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.types.EmailActionStatus;
import ar.edu.itba.paw.models.types.EmailActionType;
import java.time.Instant;

public class EmailActionRequest {

    private final Long id;
    private final EmailActionType actionType;
    private final String email;
    private final Long userId;
    private final String tokenHash;
    private final String payloadJson;
    private final EmailActionStatus status;
    private final Instant expiresAt;
    private final Instant consumedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public EmailActionRequest(
            final Long id,
            final EmailActionType actionType,
            final String email,
            final Long userId,
            final String tokenHash,
            final String payloadJson,
            final EmailActionStatus status,
            final Instant expiresAt,
            final Instant consumedAt,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.actionType = actionType;
        this.email = email;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.payloadJson = payloadJson;
        this.status = status;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public EmailActionType getActionType() {
        return actionType;
    }

    public String getEmail() {
        return email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public EmailActionStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isExpired(final Instant now) {
        return expiresAt.isBefore(now) || expiresAt.equals(now);
    }

    @Override
    public String toString() {
        return "EmailActionRequest{"
                + "id="
                + id
                + ", actionType="
                + actionType
                + ", userId="
                + userId
                + ", status="
                + status
                + ", expiresAt="
                + expiresAt
                + ", consumedAt="
                + consumedAt
                + ", createdAt="
                + createdAt
                + ", updatedAt="
                + updatedAt
                + ", hasEmail="
                + (email != null && !email.isBlank())
                + ", hasTokenHash="
                + (tokenHash != null && !tokenHash.isBlank())
                + ", hasPayload="
                + (payloadJson != null && !payloadJson.isBlank())
                + '}';
    }
}
