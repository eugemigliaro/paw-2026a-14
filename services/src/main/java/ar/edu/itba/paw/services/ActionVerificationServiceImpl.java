package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.EmailActionStatus;
import ar.edu.itba.paw.models.EmailActionType;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchReservationPayload;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.EmailActionRequestDao;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.mail.MailService;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import ar.edu.itba.paw.services.mail.VerificationMailTemplateData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionVerificationServiceImpl implements ActionVerificationService {

    private static final DateTimeFormatter MATCH_SCHEDULE_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(Locale.US);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailActionRequestDao emailActionRequestDao;
    private final MatchDao matchDao;
    private final UserService userService;
    private final MailProperties mailProperties;
    private final MailService mailService;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ActionVerificationServiceImpl(
            final EmailActionRequestDao emailActionRequestDao,
            final MatchDao matchDao,
            final UserService userService,
            final MailProperties mailProperties,
            final MailService mailService,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final ObjectMapper objectMapper,
            final Clock clock) {
        this.emailActionRequestDao = emailActionRequestDao;
        this.matchDao = matchDao;
        this.userService = userService;
        this.mailProperties = mailProperties;
        this.mailService = mailService;
        this.templateRenderer = templateRenderer;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public VerificationRequestResult requestMatchReservation(
            final Long matchId, final String email) {
        final String normalizedEmail = normalizeEmail(email);
        final Match match =
                matchDao.findPublicMatchById(matchId)
                        .orElseThrow(
                                () ->
                                        new VerificationFailureException(
                                                VerificationFailureReason.INVALID_ACTION,
                                                "This event is no longer available for reservations."));

        if (match.getAvailableSpots() <= 0) {
            throw new VerificationFailureException(
                    VerificationFailureReason.INVALID_ACTION,
                    "This event is already full.");
        }

        final Optional<User> existingUser = userService.findByEmail(normalizedEmail);
        if (existingUser.isPresent()
                && matchDao.hasActiveParticipant(matchId, existingUser.get().getId())) {
            throw new VerificationFailureException(
                    VerificationFailureReason.INVALID_ACTION,
                    "This email already has a confirmed reservation for the event.");
        }

        final Instant expiresAt =
                Instant.now(clock).plusSeconds(mailProperties.getVerificationTtlHours() * 3600L);
        final String rawToken = generateToken();
        final String tokenHash = hashToken(rawToken);
        final String payloadJson = serializePayload(new MatchReservationPayload(matchId));

        emailActionRequestDao.create(
                EmailActionType.MATCH_RESERVATION,
                normalizedEmail,
                existingUser.map(User::getId).orElse(null),
                tokenHash,
                payloadJson,
                expiresAt);

        final VerificationPreview preview = buildReservationPreview(match, normalizedEmail, expiresAt);
        final String confirmationUrl = buildConfirmationUrl(rawToken);
        final MailContent mailContent =
                templateRenderer.renderReservationConfirmation(
                        new VerificationMailTemplateData(
                                preview.getTitle(),
                                preview.getSummary(),
                                normalizedEmail,
                                confirmationUrl,
                                expiresAt,
                                preview.getDetails()));
        mailService.send(normalizedEmail, mailContent);

        return new VerificationRequestResult(normalizedEmail, expiresAt);
    }

    @Override
    public VerificationPreview getPreview(final String rawToken) {
        final EmailActionRequest request = getRequiredPendingRequest(rawToken, false);
        final MatchReservationPayload payload = deserializePayload(request.getPayloadJson());
        final Match match =
                matchDao.findPublicMatchById(payload.getMatchId())
                        .orElseThrow(
                                () ->
                                        invalidateRequest(
                                                request,
                                                request.getUserId(),
                                                "This event is no longer available for reservation."));
        return buildReservationPreview(match, request.getEmail(), request.getExpiresAt());
    }

    @Override
    @Transactional
    public VerificationConfirmationResult confirm(final String rawToken) {
        final EmailActionRequest request = getRequiredPendingRequest(rawToken, true);
        final MatchReservationPayload payload = deserializePayload(request.getPayloadJson());
        final Match match =
                matchDao.findPublicMatchById(payload.getMatchId())
                        .orElseThrow(
                                () ->
                                        invalidateRequest(
                                                request,
                                                request.getUserId(),
                                                "This event is no longer available for reservation."));

        final User user = resolveUser(request.getEmail());
        final Long userId = user.getId();

        if (matchDao.hasActiveParticipant(match.getId(), userId)) {
            throw invalidateRequest(
                    request,
                    userId,
                    "This email already has a confirmed reservation for the event.");
        }

        if (!matchDao.addParticipantIfSpace(match.getId(), userId)) {
            final String failureMessage =
                    matchDao.findPublicMatchById(match.getId()).isEmpty()
                            ? "This event is no longer available for reservation."
                            : "The event filled up before the reservation could be confirmed.";
            throw invalidateRequest(request, userId, failureMessage);
        }

        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.COMPLETED, userId, Instant.now(clock));

        return new VerificationConfirmationResult(
                userId,
                "/events/" + match.getId() + "?reservation=confirmed",
                "Your reservation is now confirmed.");
    }

    private EmailActionRequest getRequiredPendingRequest(
            final String rawToken, final boolean forUpdate) {
        final String tokenHash = hashToken(rawToken);
        final EmailActionRequest request =
                (forUpdate
                                ? emailActionRequestDao.findByTokenHashForUpdate(tokenHash)
                                : emailActionRequestDao.findByTokenHash(tokenHash))
                        .orElseThrow(
                                () ->
                                        new VerificationFailureException(
                                                VerificationFailureReason.NOT_FOUND,
                                                "That verification link is invalid or no longer exists."));

        if (request.getStatus() == EmailActionStatus.COMPLETED
                || request.getStatus() == EmailActionStatus.FAILED) {
            throw new VerificationFailureException(
                    VerificationFailureReason.ALREADY_USED,
                    "That verification link was already used.");
        }

        if (request.getStatus() == EmailActionStatus.EXPIRED || request.isExpired(Instant.now(clock))) {
            emailActionRequestDao.updateStatus(
                    request.getId(),
                    EmailActionStatus.EXPIRED,
                    request.getUserId(),
                    Instant.now(clock));
            throw new VerificationFailureException(
                    VerificationFailureReason.EXPIRED,
                    "That verification link has expired.");
        }

        return request;
    }

    private VerificationPreview buildReservationPreview(
            final Match match, final String email, final Instant expiresAt) {
        return new VerificationPreview(
                "Confirm your reservation for " + match.getTitle(),
                "Use this one-time confirmation to reserve your spot in the event.",
                email,
                expiresAt,
                "Confirm reservation",
                "/events/" + match.getId() + "?reservation=confirmed",
                List.of(
                        new VerificationPreviewDetail("Sport", match.getSport().getDisplayName()),
                        new VerificationPreviewDetail("Venue", match.getAddress()),
                        new VerificationPreviewDetail(
                                "Schedule",
                                MATCH_SCHEDULE_FORMATTER.format(
                                        match.getStartsAt().atZone(ZoneId.systemDefault()))),
                        new VerificationPreviewDetail("Price", toPriceLabel(match.getPricePerPlayer())),
                        new VerificationPreviewDetail(
                                "Spots left", String.valueOf(match.getAvailableSpots()))));
    }

    private User resolveUser(final String email) {
        return userService.findByEmail(email).orElseGet(() -> createUserForEmail(email));
    }

    private User createUserForEmail(final String email) {
        final String baseUsername = sanitizeUsername(email);
        String candidate = baseUsername;
        int suffix = 1;

        while (userService.findByUsername(candidate).isPresent()) {
            candidate = truncateUsername(baseUsername, suffix) + suffix;
            suffix++;
        }

        return userService.createUser(email, candidate);
    }

    private static String sanitizeUsername(final String email) {
        final String localPart = email.split("@", 2)[0].toLowerCase(Locale.ROOT);
        final String sanitized = localPart.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? "player" : truncateUsername(sanitized, 0);
    }

    private static String truncateUsername(final String username, final int suffix) {
        final int maxLength = 50;
        final int reserved = suffix == 0 ? 0 : String.valueOf(suffix).length();
        final int limit = Math.max(1, maxLength - reserved);
        return username.length() <= limit ? username : username.substring(0, limit);
    }

    private String buildConfirmationUrl(final String rawToken) {
        return stripTrailingSlash(mailProperties.getBaseUrl()) + "/verifications/" + rawToken;
    }

    private static String stripTrailingSlash(final String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private MatchReservationPayload deserializePayload(final String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, MatchReservationPayload.class);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize verification payload", exception);
        }
    }

    private String serializePayload(final MatchReservationPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize verification payload", exception);
        }
    }

    private VerificationFailureException invalidateRequest(
            final EmailActionRequest request, final Long userId, final String message) {
        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.FAILED, userId, Instant.now(clock));
        return new VerificationFailureException(VerificationFailureReason.INVALID_ACTION, message);
    }

    private static String normalizeEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email cannot be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String generateToken() {
        final byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(final String rawToken) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String toPriceLabel(final BigDecimal pricePerPlayer) {
        if (pricePerPlayer == null) {
            return "Price TBD";
        }
        return pricePerPlayer.compareTo(BigDecimal.ZERO) == 0 ? "Free" : "$" + pricePerPlayer;
    }
}
