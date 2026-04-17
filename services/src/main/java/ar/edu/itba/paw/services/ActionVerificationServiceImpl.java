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
import ar.edu.itba.paw.services.exceptions.VerificationFailureException;
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
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionVerificationServiceImpl implements ActionVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailActionRequestDao emailActionRequestDao;
    private final MatchDao matchDao;
    private final MatchService matchService;
    private final MvpIdentityService mvpIdentityService;
    private final MatchReservationService matchReservationService;
    private final MailProperties mailProperties;
    private final MailDispatchService mailDispatchService;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final MessageSource messageSource;
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
            final MessageSource messageSource,
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
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public VerificationRequestResult requestMatchReservation(
            final Long matchId, final String email) {
        final Locale locale = currentLocale();
        final String normalizedEmail = normalizeEmail(email);
        final Match match =
                matchDao.findPublicMatchById(matchId)
                        .orElseThrow(
                                () ->
                                        new VerificationFailureException(
                                                VerificationFailureReason.INVALID_ACTION,
                                                message(
                                                        "verification.message.reservationUnavailable",
                                                        locale)));

        if (match.getAvailableSpots() <= 0) {
            throw new VerificationFailureException(
                    VerificationFailureReason.INVALID_ACTION,
                    message("verification.message.eventFull", locale));
        }

        final Optional<User> existingUser = mvpIdentityService.findExistingByEmail(normalizedEmail);
        if (existingUser.isPresent()
                && matchReservationService.hasActiveReservation(
                        matchId, existingUser.get().getId())) {
            throw new VerificationFailureException(
                    VerificationFailureReason.INVALID_ACTION,
                    message("verification.message.alreadyReserved", locale));
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
                buildReservationPreview(match, normalizedEmail, expiresAt, locale);
        final String confirmationUrl = buildConfirmationUrl(rawToken, locale);
        final MailContent mailContent =
                templateRenderer.renderReservationConfirmation(
                        new VerificationMailTemplateData(
                                preview.getTitle(),
                                preview.getSummary(),
                                normalizedEmail,
                                confirmationUrl,
                                expiresAt,
                                preview.getDetails(),
                                locale));
        mailDispatchService.dispatch(normalizedEmail, mailContent);

        return new VerificationRequestResult(normalizedEmail, expiresAt);
    }

    @Override
    public VerificationRequestResult requestMatchCreation(
            final CreateMatchRequest request, final String email) {
        final Locale locale = currentLocale();
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
                        deserializeMatchCreationPayload(payloadJson),
                        normalizedEmail,
                        expiresAt,
                        locale);
        final String confirmationUrl = buildConfirmationUrl(rawToken, locale);
        final MailContent mailContent =
                templateRenderer.renderReservationConfirmation(
                        new VerificationMailTemplateData(
                                preview.getTitle(),
                                preview.getSummary(),
                                normalizedEmail,
                                confirmationUrl,
                                expiresAt,
                                preview.getDetails(),
                                locale));
        mailDispatchService.dispatch(normalizedEmail, mailContent);

        return new VerificationRequestResult(normalizedEmail, expiresAt);
    }

    @Override
    public VerificationPreview getPreview(final String rawToken) {
        final Locale locale = currentLocale();
        final EmailActionRequest request = getRequiredPendingRequest(rawToken, false, locale);
        if (request.getActionType() == EmailActionType.MATCH_CREATION) {
            final MatchCreationPayload payload =
                    deserializeMatchCreationPayload(request.getPayloadJson());
            return buildMatchCreationPreview(
                    payload, request.getEmail(), request.getExpiresAt(), locale);
        }

        final MatchReservationPayload payload = deserializePayload(request.getPayloadJson());
        final Match match =
                matchDao.findPublicMatchById(payload.getMatchId())
                        .orElseThrow(
                                () ->
                                        invalidateRequest(
                                                request,
                                                request.getUserId(),
                                                message(
                                                        "verification.message.reservationUnavailable",
                                                        locale)));
        return buildReservationPreview(match, request.getEmail(), request.getExpiresAt(), locale);
    }

    @Override
    @Transactional
    public VerificationConfirmationResult confirm(final String rawToken) {
        final Locale locale = currentLocale();
        final EmailActionRequest request = getRequiredPendingRequest(rawToken, true, locale);
        if (request.getActionType() == EmailActionType.MATCH_CREATION) {
            return confirmMatchCreation(request, locale);
        }

        final MatchReservationPayload payload = deserializePayload(request.getPayloadJson());
        final Match match =
                matchDao.findPublicMatchById(payload.getMatchId())
                        .orElseThrow(
                                () ->
                                        invalidateRequest(
                                                request,
                                                request.getUserId(),
                                                message(
                                                        "verification.message.reservationUnavailable",
                                                        locale)));

        final User user = mvpIdentityService.resolveOrCreateByEmail(request.getEmail());
        final Long userId = user.getId();

        try {
            matchReservationService.reserveSpot(match.getId(), userId);
        } catch (final MatchReservationException exception) {
            throw invalidateRequest(
                    request, userId, reservationErrorMessage(exception.getCode(), locale));
        }

        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.COMPLETED, userId, Instant.now(clock));

        return new VerificationConfirmationResult(
                userId,
                "/matches/" + match.getId() + "?reservation=confirmed",
                message("verification.message.reservationConfirmed", locale));
    }

    private VerificationConfirmationResult confirmMatchCreation(
            final EmailActionRequest request, final Locale locale) {
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
                user.getId(),
                "/matches/" + createdMatch.getId(),
                message("verification.message.eventPublished", locale));
    }

    private EmailActionRequest getRequiredPendingRequest(
            final String rawToken, final boolean forUpdate, final Locale locale) {
        final String tokenHash = hashToken(rawToken);
        final EmailActionRequest request =
                (forUpdate
                                ? emailActionRequestDao.findByTokenHashForUpdate(tokenHash)
                                : emailActionRequestDao.findByTokenHash(tokenHash))
                        .orElseThrow(
                                () ->
                                        new VerificationFailureException(
                                                VerificationFailureReason.NOT_FOUND,
                                                message("verification.message.notFound", locale)));

        if (request.getStatus() == EmailActionStatus.COMPLETED
                || request.getStatus() == EmailActionStatus.FAILED) {
            throw new VerificationFailureException(
                    VerificationFailureReason.ALREADY_USED,
                    message("verification.message.alreadyUsed", locale));
        }

        if (request.getStatus() == EmailActionStatus.EXPIRED
                || request.isExpired(Instant.now(clock))) {
            emailActionRequestDao.updateStatus(
                    request.getId(),
                    EmailActionStatus.EXPIRED,
                    request.getUserId(),
                    Instant.now(clock));
            throw new VerificationFailureException(
                    VerificationFailureReason.EXPIRED,
                    message("verification.message.expired", locale));
        }

        return request;
    }

    private VerificationPreview buildReservationPreview(
            final Match match, final String email, final Instant expiresAt, final Locale locale) {
        return new VerificationPreview(
                message(
                        "verification.preview.reservation.title",
                        new Object[] {match.getTitle()},
                        locale),
                message("verification.preview.reservation.summary", locale),
                email,
                expiresAt,
                message("verification.preview.reservation.confirm", locale),
                "/matches/" + match.getId() + "?reservation=confirmed",
                List.of(
                        new VerificationPreviewDetail(
                                message("verification.preview.detail.sport", locale),
                                toSportLabel(match.getSport(), locale)),
                        new VerificationPreviewDetail(
                                message("verification.preview.detail.venue", locale),
                                match.getAddress()),
                        new VerificationPreviewDetail(
                                message("verification.preview.detail.schedule", locale),
                                formatSchedule(match.getStartsAt(), locale)),
                        new VerificationPreviewDetail(
                                message("verification.preview.detail.price", locale),
                                toPriceLabel(match.getPricePerPlayer(), locale)),
                        new VerificationPreviewDetail(
                                message("verification.preview.detail.spotsLeft", locale),
                                String.valueOf(match.getAvailableSpots()))));
    }

    private String buildConfirmationUrl(final String rawToken, final Locale locale) {
        final String baseUrl =
                stripTrailingSlash(mailProperties.getBaseUrl()) + "/verifications/" + rawToken;
        final String languageTag = resolvedLocale(locale).getLanguage();
        return languageTag.isBlank() ? baseUrl : baseUrl + "?lang=" + languageTag;
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

    private String toPriceLabel(final BigDecimal pricePerPlayer, final Locale locale) {
        if (pricePerPlayer == null) {
            return message("price.tbd", locale);
        }
        return pricePerPlayer.compareTo(BigDecimal.ZERO) == 0
                ? message("price.free", locale)
                : message("price.amount", new Object[] {pricePerPlayer}, locale);
    }

    private VerificationPreview buildMatchCreationPreview(
            final MatchCreationPayload payload,
            final String email,
            final Instant expiresAt,
            final Locale locale) {
        final List<VerificationPreviewDetail> details = new ArrayList<>();
        details.add(
                new VerificationPreviewDetail(
                        message("verification.preview.detail.sport", locale),
                        toSportLabel(payload.getSport(), locale)));
        details.add(
                new VerificationPreviewDetail(
                        message("verification.preview.detail.title", locale),
                        safeValue(payload.getTitle())));
        details.add(
                new VerificationPreviewDetail(
                        message("verification.preview.detail.venue", locale),
                        safeValue(payload.getAddress())));
        details.add(
                new VerificationPreviewDetail(
                        message("verification.preview.detail.schedule", locale),
                        formatSchedule(
                                Instant.ofEpochMilli(payload.getStartsAtEpochMillis()), locale)));
        if (payload.getEndsAtEpochMillis() != null) {
            details.add(
                    new VerificationPreviewDetail(
                            message("verification.preview.detail.endTime", locale),
                            formatEndTime(payload.getEndsAtEpochMillis(), locale)));
        }
        details.add(
                new VerificationPreviewDetail(
                        message("verification.preview.detail.price", locale),
                        toPriceLabel(payload.getPricePerPlayer(), locale)));
        details.add(
                new VerificationPreviewDetail(
                        message("verification.preview.detail.capacity", locale),
                        String.valueOf(payload.getMaxPlayers())));

        return new VerificationPreview(
                message("verification.preview.creation.title", locale),
                message("verification.preview.creation.summary", locale),
                email,
                expiresAt,
                message("verification.preview.creation.confirm", locale),
                "/host/matches/new",
                details);
    }

    private static String safeValue(final String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String toSportLabel(final Sport sport, final Locale locale) {
        return message(
                "sport." + sport.getDbValue(),
                null,
                resolvedLocale(locale),
                sport.getDisplayName());
    }

    private String toSportLabel(final String sport, final Locale locale) {
        if (sport == null || sport.isBlank()) {
            return toSportLabel(Sport.PADEL, locale);
        }
        return Sport.fromDbValue(sport)
                .map(value -> toSportLabel(value, locale))
                .orElse(toSportLabel(Sport.PADEL, locale));
    }

    private String reservationErrorMessage(final String code, final Locale locale) {
        switch (code) {
            case "closed":
                return message("reservation.error.closed", locale);
            case "started":
                return message("reservation.error.started", locale);
            case "already_joined":
                return message("reservation.error.alreadyJoined", locale);
            case "full":
                return message("reservation.error.fullBeforeConfirm", locale);
            case "not_found":
            default:
                return message("reservation.error.notFound", locale);
        }
    }

    private String formatSchedule(final Instant startsAt, final Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(resolvedLocale(locale))
                .format(startsAt.atZone(ZoneId.systemDefault()));
    }

    private String formatEndTime(final long endsAtEpochMillis, final Locale locale) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(resolvedLocale(locale))
                .format(Instant.ofEpochMilli(endsAtEpochMillis).atZone(ZoneId.systemDefault()));
    }

    private Locale currentLocale() {
        return resolvedLocale(LocaleContextHolder.getLocale());
    }

    private static Locale resolvedLocale(final Locale locale) {
        return locale == null ? Locale.ENGLISH : locale;
    }

    private String message(final String code, final Locale locale) {
        return message(code, null, locale);
    }

    private String message(final String code, final Object[] args, final Locale locale) {
        return message(code, args, resolvedLocale(locale), code);
    }

    private String message(
            final String code,
            final Object[] args,
            final Locale locale,
            final String defaultMessage) {
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }
}
