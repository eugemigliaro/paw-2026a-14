package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.EmailActionStatus;
import ar.edu.itba.paw.models.EmailActionType;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchCreationPayload;
import ar.edu.itba.paw.models.MatchReservationPayload;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.EmailActionRequestDao;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MailProperties;
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
import java.util.ArrayList;
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
    private static final DateTimeFormatter MATCH_END_TIME_FORMATTER =
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.US);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailActionRequestDao emailActionRequestDao;
    private final MatchDao matchDao;
    private final MatchService matchService;
    private final MvpIdentityService mvpIdentityService;
    private final MatchReservationService matchReservationService;
    private final MailProperties mailProperties;
    private final MailDispatchService mailDispatchService;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ActionVerificationServiceImpl(
            final EmailActionRequestDao emailActionRequestDao,
            final MatchDao matchDao,
            final MatchService matchService,
            final MvpIdentityService mvpIdentityService,
            final MatchReservationService matchReservationService,
            final MailProperties mailProperties,
            final MailDispatchService mailDispatchService,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final ObjectMapper objectMapper,
            final Clock clock) {
        this.emailActionRequestDao = emailActionRequestDao;
        this.matchDao = matchDao;
        this.matchService = matchService;
        this.mvpIdentityService = mvpIdentityService;
        this.matchReservationService = matchReservationService;
        this.mailProperties = mailProperties;
        this.mailDispatchService = mailDispatchService;
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
                    VerificationFailureReason.INVALID_ACTION, "This event is already full.");
        }

        final Optional<User> existingUser = mvpIdentityService.findExistingByEmail(normalizedEmail);
        if (existingUser.isPresent()
                && matchReservationService.hasActiveReservation(
                        matchId, existingUser.get().getId())) {
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

        final VerificationPreview preview =
                buildReservationPreview(match, normalizedEmail, expiresAt);
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
        mailDispatchService.dispatch(normalizedEmail, mailContent);

        return new VerificationRequestResult(normalizedEmail, expiresAt);
    }

    @Override
    public VerificationRequestResult requestMatchCreation(
            final CreateMatchRequest request, final String email) {
        final String normalizedEmail = normalizeEmail(email);
        final Instant expiresAt =
                Instant.now(clock).plusSeconds(mailProperties.getVerificationTtlHours() * 3600L);
        final String rawToken = generateToken();
        final String tokenHash = hashToken(rawToken);
        final String payloadJson =
                serializePayload(
                        new MatchCreationPayload(
                                null,
                                request.getAddress(),
                                request.getTitle(),
                                request.getDescription(),
                                request.getStartsAt() == null
                                        ? null
                                        : request.getStartsAt().toEpochMilli(),
                                request.getEndsAt() == null
                                        ? null
                                        : request.getEndsAt().toEpochMilli(),
                                request.getMaxPlayers(),
                                request.getPricePerPlayer(),
                                request.getSport() == null ? null : request.getSport().getDbValue(),
                                request.getVisibility(),
                                request.getStatus(),
                                request.getBannerImageId()));

        emailActionRequestDao.create(
                EmailActionType.MATCH_CREATION,
                normalizedEmail,
                mvpIdentityService
                        .findExistingByEmail(normalizedEmail)
                        .map(User::getId)
                        .orElse(null),
                tokenHash,
                payloadJson,
                expiresAt);

        final VerificationPreview preview =
                buildMatchCreationPreview(
                        deserializeMatchCreationPayload(payloadJson), normalizedEmail, expiresAt);
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
        mailDispatchService.dispatch(normalizedEmail, mailContent);

        return new VerificationRequestResult(normalizedEmail, expiresAt);
    }

    @Override
    public VerificationPreview getPreview(final String rawToken) {
        final EmailActionRequest request = getRequiredPendingRequest(rawToken, false);
        if (request.getActionType() == EmailActionType.MATCH_CREATION) {
            final MatchCreationPayload payload =
                    deserializeMatchCreationPayload(request.getPayloadJson());
            return buildMatchCreationPreview(payload, request.getEmail(), request.getExpiresAt());
        }

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
        if (request.getActionType() == EmailActionType.MATCH_CREATION) {
            return confirmMatchCreation(request);
        }

        final MatchReservationPayload payload = deserializePayload(request.getPayloadJson());
        final Match match =
                matchDao.findPublicMatchById(payload.getMatchId())
                        .orElseThrow(
                                () ->
                                        invalidateRequest(
                                                request,
                                                request.getUserId(),
                                                "This event is no longer available for reservation."));

        final User user = mvpIdentityService.resolveOrCreateByEmail(request.getEmail());
        final Long userId = user.getId();

        try {
            matchReservationService.reserveSpot(match.getId(), userId);
        } catch (final MatchReservationException exception) {
            throw invalidateRequest(request, userId, exception.getMessage());
        }

        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.COMPLETED, userId, Instant.now(clock));

        return new VerificationConfirmationResult(
                userId,
                "/matches/" + match.getId() + "?reservation=confirmed",
                "Your reservation is now confirmed.");
    }

    private VerificationConfirmationResult confirmMatchCreation(final EmailActionRequest request) {
        final MatchCreationPayload payload =
                deserializeMatchCreationPayload(request.getPayloadJson());
        final User user = mvpIdentityService.resolveOrCreateByEmail(request.getEmail());

        final Sport sport =
                payload.getSport() == null
                        ? Sport.PADEL
                        : Sport.fromDbValue(payload.getSport()).orElse(Sport.PADEL);

        final Match createdMatch =
                matchService.createMatch(
                        new CreateMatchRequest(
                                user.getId(),
                                payload.getAddress(),
                                payload.getTitle(),
                                payload.getDescription(),
                                Instant.ofEpochMilli(payload.getStartsAtEpochMillis()),
                                payload.getEndsAtEpochMillis() == null
                                        ? null
                                        : Instant.ofEpochMilli(payload.getEndsAtEpochMillis()),
                                payload.getMaxPlayers(),
                                payload.getPricePerPlayer(),
                                sport,
                                payload.getVisibility(),
                                payload.getStatus(),
                                payload.getBannerImageId()));

        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.COMPLETED, user.getId(), Instant.now(clock));

        return new VerificationConfirmationResult(
                user.getId(), "/matches/" + createdMatch.getId(), "Your match is now published.");
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

        if (request.getStatus() == EmailActionStatus.EXPIRED
                || request.isExpired(Instant.now(clock))) {
            emailActionRequestDao.updateStatus(
                    request.getId(),
                    EmailActionStatus.EXPIRED,
                    request.getUserId(),
                    Instant.now(clock));
            throw new VerificationFailureException(
                    VerificationFailureReason.EXPIRED, "That verification link has expired.");
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
                "/matches/" + match.getId() + "?reservation=confirmed",
                List.of(
                        new VerificationPreviewDetail("Sport", match.getSport().getDisplayName()),
                        new VerificationPreviewDetail("Venue", match.getAddress()),
                        new VerificationPreviewDetail(
                                "Schedule",
                                MATCH_SCHEDULE_FORMATTER.format(
                                        match.getStartsAt().atZone(ZoneId.systemDefault()))),
                        new VerificationPreviewDetail(
                                "Price", toPriceLabel(match.getPricePerPlayer())),
                        new VerificationPreviewDetail(
                                "Spots left", String.valueOf(match.getAvailableSpots()))));
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
            throw new IllegalStateException(
                    "Failed to deserialize verification payload", exception);
        }
    }

    private MatchCreationPayload deserializeMatchCreationPayload(final String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, MatchCreationPayload.class);
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to deserialize verification payload", exception);
        }
    }

    private String serializePayload(final Object payload) {
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

    private VerificationPreview buildMatchCreationPreview(
            final MatchCreationPayload payload, final String email, final Instant expiresAt) {
        final List<VerificationPreviewDetail> details = new ArrayList<>();
        details.add(new VerificationPreviewDetail("Sport", prettySport(payload.getSport())));
        details.add(new VerificationPreviewDetail("Title", safeValue(payload.getTitle())));
        details.add(new VerificationPreviewDetail("Venue", safeValue(payload.getAddress())));
        details.add(new VerificationPreviewDetail("Schedule", formatStartSchedule(payload)));
        if (payload.getEndsAtEpochMillis() != null) {
            details.add(new VerificationPreviewDetail("End time", formatEndTime(payload)));
        }
        details.add(
                new VerificationPreviewDetail("Price", toPriceLabel(payload.getPricePerPlayer())));
        details.add(
                new VerificationPreviewDetail("Capacity", String.valueOf(payload.getMaxPlayers())));

        return new VerificationPreview(
                "Confirm your match publication",
                "Use this one-time confirmation to publish your match.",
                email,
                expiresAt,
                "Confirm match publication",
                "/host/matches/new",
                details);
    }

    private static String safeValue(final String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String prettySport(final String sport) {
        if (sport == null || sport.isBlank()) {
            return "Padel";
        }
        return Sport.fromDbValue(sport).map(Sport::getDisplayName).orElse("Padel");
    }

    private static String formatStartSchedule(final MatchCreationPayload payload) {
        final ZoneId zoneId = ZoneId.systemDefault();
        return MATCH_SCHEDULE_FORMATTER.format(
                Instant.ofEpochMilli(payload.getStartsAtEpochMillis()).atZone(zoneId));
    }

    private static String formatEndTime(final MatchCreationPayload payload) {
        return MATCH_END_TIME_FORMATTER.format(
                Instant.ofEpochMilli(payload.getEndsAtEpochMillis())
                        .atZone(ZoneId.systemDefault()));
    }
}
