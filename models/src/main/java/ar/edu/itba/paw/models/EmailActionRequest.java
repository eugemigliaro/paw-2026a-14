package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.EmailActionStatusConverter;
import ar.edu.itba.paw.models.converters.EmailActionTypeConverter;
import ar.edu.itba.paw.models.types.EmailActionStatus;
import ar.edu.itba.paw.models.types.EmailActionType;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "email_action_requests")
public class EmailActionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_action_requests_id_seq")
    @SequenceGenerator(
            sequenceName = "email_action_requests_id_seq",
            name = "email_action_requests_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "action_type", length = 50, nullable = false)
    @Convert(converter = EmailActionTypeConverter.class)
    private EmailActionType actionType;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token_hash", length = 128, nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "payload_json", columnDefinition = "text", nullable = false)
    private String payloadJson;

    @Column(name = "status", length = 20, nullable = false)
    @Convert(converter = EmailActionStatusConverter.class)
    private EmailActionStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    EmailActionRequest() {}

    public EmailActionRequest(
            final Long id,
            final EmailActionType actionType,
            final String email,
            final User user,
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
        this.user = user;
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

    public User getUser() {
        return user;
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

    public void setUser(final User user) {
        this.user = user;
    }

    public void setStatus(final EmailActionStatus status) {
        this.status = status;
    }

    public void setConsumedAt(final Instant consumedAt) {
        this.consumedAt = consumedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
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
                + (user == null ? null : user.getId())
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
