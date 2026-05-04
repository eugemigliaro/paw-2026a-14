package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.SecurityControllerUtils.requireAuthenticatedUserId;

import ar.edu.itba.paw.models.EventJoinPolicy;
import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.RecurrenceEndMode;
import ar.edu.itba.paw.models.RecurrenceFrequency;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.CreateRecurrenceRequest;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.MatchUpdateFailureReason;
import ar.edu.itba.paw.services.UpdateMatchRequest;
import ar.edu.itba.paw.services.exceptions.MatchCancellationException;
import ar.edu.itba.paw.services.exceptions.MatchUpdateException;
import ar.edu.itba.paw.webapp.form.CreateEventForm;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
@PreAuthorize("isAuthenticated()")
public class HostController {

    private static final double DEFAULT_MAP_LATITUDE = -34.6037;
    private static final double DEFAULT_MAP_LONGITUDE = -58.3816;
    private static final int DEFAULT_MAP_ZOOM = 14;

    private final MatchService matchService;
    private final ImageService imageService;
    private final Clock clock;
    private final MessageSource messageSource;
    private final boolean mapPickerEnabled;
    private final String mapTileUrlTemplate;
    private final String mapAttribution;
    private final double mapDefaultLatitude;
    private final double mapDefaultLongitude;
    private final int mapDefaultZoom;

    @Autowired
    public HostController(
            final MatchService matchService,
            final ImageService imageService,
            final Clock clock,
            final MessageSource messageSource,
            @Value("${map.picker.enabled:false}") final boolean mapPickerEnabled,
            @Value("${map.tiles.urlTemplate:}") final String mapTileUrlTemplate,
            @Value("${map.tiles.attribution:}") final String mapAttribution,
            @Value("${map.default.latitude:" + DEFAULT_MAP_LATITUDE + "}")
                    final double mapDefaultLatitude,
            @Value("${map.default.longitude:" + DEFAULT_MAP_LONGITUDE + "}")
                    final double mapDefaultLongitude,
            @Value("${map.default.zoom:" + DEFAULT_MAP_ZOOM + "}") final int mapDefaultZoom) {
        this.matchService = matchService;
        this.imageService = imageService;
        this.clock = clock;
        this.messageSource = messageSource;
        this.mapPickerEnabled = mapPickerEnabled;
        this.mapTileUrlTemplate = mapTileUrlTemplate == null ? "" : mapTileUrlTemplate;
        this.mapAttribution = mapAttribution == null ? "" : mapAttribution;
        this.mapDefaultLatitude = mapDefaultLatitude;
        this.mapDefaultLongitude = mapDefaultLongitude;
        this.mapDefaultZoom = mapDefaultZoom;
    }

    public HostController(
            final MatchService matchService,
            final ImageService imageService,
            final Clock clock,
            final MessageSource messageSource) {
        this(
                matchService,
                imageService,
                clock,
                messageSource,
                false,
                "",
                "",
                DEFAULT_MAP_LATITUDE,
                DEFAULT_MAP_LONGITUDE,
                DEFAULT_MAP_ZOOM);
    }

    @ModelAttribute("createEventForm")
    public CreateEventForm createEventForm() {
        return new CreateEventForm();
    }

    @GetMapping("/host/matches/new")
    public ModelAndView showCreateEvent(final Locale locale) {
        return hostFormView(createEventForm(), null, locale, createFormConfig(locale));
    }

    @PostMapping("/host/matches/new")
    public ModelAndView publishEvent(
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale) {

        final Long actingUserId = requireAuthenticatedUserId();
        final HostFormConfig formConfig = createFormConfig(locale);

        applyScheduleValidation(createEventForm, bindingResult, locale);
        validateVisibilityAndJoinPolicy(createEventForm, bindingResult, locale);
        validateCoordinates(createEventForm, bindingResult, locale);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        final Instant startsAt =
                toInstant(
                        createEventForm.getEventDate(),
                        createEventForm.getEventTime(),
                        createEventForm.getTz());
        final Instant endsAt =
                toInstant(
                        createEventForm.getEndDate(),
                        createEventForm.getEndTime(),
                        createEventForm.getTz());

        final Long bannerImageId;
        try {
            bannerImageId = storeBannerIfPresent(createEventForm, null);
        } catch (final IllegalArgumentException exception) {
            return hostFormView(createEventForm, exception.getMessage(), locale, formConfig);
        } catch (final IOException exception) {
            return hostFormView(
                    createEventForm,
                    messageSource.getMessage("host.imageError", null, locale),
                    locale,
                    formConfig);
        }

        final EventVisibility visibility =
                EventVisibility.fromDbValue(normalize(createEventForm.getVisibility()))
                        .orElse(null);
        final EventJoinPolicy joinPolicy =
                EventJoinPolicy.fromDbValue(normalize(createEventForm.getJoinPolicy()))
                        .orElse(null);

        final CreateMatchRequest request =
                new CreateMatchRequest(
                        actingUserId,
                        createEventForm.getAddress(),
                        parseCoordinate(createEventForm.getLatitude()),
                        parseCoordinate(createEventForm.getLongitude()),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        startsAt,
                        endsAt,
                        createEventForm.getMaxPlayers(),
                        createEventForm.getPricePerPlayer(),
                        Sport.fromDbValue(createEventForm.getSport()).orElse(Sport.PADEL),
                        visibility,
                        joinPolicy,
                        EventStatus.OPEN,
                        bannerImageId,
                        toRecurrenceRequest(createEventForm));

        final Match createdMatch;
        try {
            createdMatch = matchService.createMatch(request);
        } catch (final IllegalArgumentException exception) {
            return hostFormView(createEventForm, exception.getMessage(), locale, formConfig);
        }

        return new ModelAndView("redirect:/matches/" + createdMatch.getId());
    }

    @GetMapping("/host/matches/{matchId:\\d+}/edit")
    @PreAuthorize("@securityService.isHost(#matchId)")
    public ModelAndView showEditEvent(
            @PathVariable("matchId") final Long matchId, final Locale locale) {
        final Long actingUserId = requireAuthenticatedUserId();
        final Match match = findOwnedEditableMatchOrThrowNotFound(matchId, actingUserId);
        return hostFormView(toForm(match), null, locale, editFormConfig(match, locale));
    }

    @PostMapping("/host/matches/{matchId:\\d+}/edit")
    @PreAuthorize("@securityService.isHost(#matchId)")
    public ModelAndView updateEvent(
            @PathVariable("matchId") final Long matchId,
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale) {
        final Long actingUserId = requireAuthenticatedUserId();
        final Match existingMatch = findOwnedEditableMatchOrThrowNotFound(matchId, actingUserId);
        final HostFormConfig formConfig = editFormConfig(existingMatch, locale);
        applyScheduleValidation(createEventForm, bindingResult, locale);
        validateVisibilityAndJoinPolicy(createEventForm, bindingResult, locale);
        validateCoordinates(createEventForm, bindingResult, locale);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        final Instant startsAt =
                toInstant(
                        createEventForm.getEventDate(),
                        createEventForm.getEventTime(),
                        createEventForm.getTz());
        final Instant endsAt =
                toInstant(
                        createEventForm.getEndDate(),
                        createEventForm.getEndTime(),
                        createEventForm.getTz());

        final Long bannerImageId;
        try {
            bannerImageId = storeBannerIfPresent(createEventForm, existingMatch.getBannerImageId());
        } catch (final IllegalArgumentException exception) {
            return hostFormView(createEventForm, exception.getMessage(), locale, formConfig);
        } catch (final IOException exception) {
            return hostFormView(
                    createEventForm,
                    messageSource.getMessage("host.imageError", null, locale),
                    locale,
                    formConfig);
        }

        final EventVisibility visibility =
                EventVisibility.fromDbValue(normalize(createEventForm.getVisibility()))
                        .orElse(null);
        final EventJoinPolicy joinPolicy =
                EventJoinPolicy.fromDbValue(normalize(createEventForm.getJoinPolicy()))
                        .orElse(null);

        final UpdateMatchRequest request =
                new UpdateMatchRequest(
                        createEventForm.getAddress(),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        startsAt,
                        endsAt,
                        createEventForm.getMaxPlayers().intValue(),
                        createEventForm.getPricePerPlayer(),
                        Sport.fromDbValue(createEventForm.getSport()).orElse(Sport.PADEL),
                        visibility,
                        joinPolicy,
                        existingMatch.getStatus(),
                        bannerImageId,
                        parseCoordinate(createEventForm.getLatitude()),
                        parseCoordinate(createEventForm.getLongitude()));

        try {
            matchService.updateMatch(matchId, actingUserId, request);
        } catch (final MatchUpdateException exception) {
            if (exception.getReason() == MatchUpdateFailureReason.MATCH_NOT_FOUND
                    || exception.getReason() == MatchUpdateFailureReason.FORBIDDEN) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (exception.getReason() == MatchUpdateFailureReason.CAPACITY_BELOW_CONFIRMED) {
                bindingResult.rejectValue(
                        "maxPlayers",
                        "match.update.error.capacityBelowConfirmed",
                        exception.getMessage());
            } else if (exception.getReason() == MatchUpdateFailureReason.CAPACITY_ABOVE_MAX) {
                bindingResult.rejectValue(
                        "maxPlayers",
                        "match.update.error.capacityAboveMax",
                        exception.getMessage());
            } else if (exception.getReason() == MatchUpdateFailureReason.NOT_EDITABLE) {
                return hostFormView(createEventForm, exception.getMessage(), locale, formConfig);
            } else if (exception.getReason() == MatchUpdateFailureReason.INVALID_SCHEDULE) {
                if (!isEndAfterStart(createEventForm)) {
                    bindingResult.rejectValue(
                            "endTime",
                            "match.schedule.error.endBeforeStart",
                            messageSource.getMessage(
                                    "match.schedule.error.endBeforeStart", null, locale));
                } else {
                    bindingResult.rejectValue(
                            "eventTime",
                            "match.schedule.error.startsAtPast",
                            messageSource.getMessage(
                                    "match.schedule.error.startsAtPast", null, locale));
                }
            } else {
                bindingResult.rejectValue(
                        "eventTime", "match.schedule.error.startsAtPast", exception.getMessage());
            }
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        return new ModelAndView("redirect:/matches/" + matchId + "?hostAction=updated");
    }

    @GetMapping("/host/matches/{matchId:\\d+}/series/edit")
    @PreAuthorize("@securityService.isHost(#matchId)")
    public ModelAndView showEditSeries(
            @PathVariable("matchId") final Long matchId, final Locale locale) {
        final Long actingUserId = requireAuthenticatedUserId();
        final Match match = findOwnedEditableRecurringMatchOrThrowNotFound(matchId, actingUserId);
        return hostFormView(toForm(match), null, locale, seriesEditFormConfig(match, locale));
    }

    @PostMapping("/host/matches/{matchId:\\d+}/series/edit")
    @PreAuthorize("@securityService.isHost(#matchId)")
    public ModelAndView updateSeries(
            @PathVariable("matchId") final Long matchId,
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale) {
        final Long actingUserId = requireAuthenticatedUserId();
        final Match existingMatch =
                findOwnedEditableRecurringMatchOrThrowNotFound(matchId, actingUserId);
        final HostFormConfig formConfig = seriesEditFormConfig(existingMatch, locale);
        applyScheduleValidation(createEventForm, bindingResult, locale);
        validateVisibilityAndJoinPolicy(createEventForm, bindingResult, locale);
        validateCoordinates(createEventForm, bindingResult, locale);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        final Long bannerImageId;
        try {
            bannerImageId = storeBannerIfPresent(createEventForm, existingMatch.getBannerImageId());
        } catch (final IllegalArgumentException exception) {
            return hostFormView(createEventForm, exception.getMessage(), locale, formConfig);
        } catch (final IOException exception) {
            return hostFormView(
                    createEventForm,
                    messageSource.getMessage("host.imageError", null, locale),
                    locale,
                    formConfig);
        }

        final UpdateMatchRequest request =
                toUpdateRequest(createEventForm, existingMatch.getStatus(), bannerImageId);

        try {
            matchService.updateSeriesFromOccurrence(matchId, actingUserId, request);
        } catch (final MatchUpdateException exception) {
            if (exception.getReason() == MatchUpdateFailureReason.MATCH_NOT_FOUND
                    || exception.getReason() == MatchUpdateFailureReason.FORBIDDEN
                    || exception.getReason() == MatchUpdateFailureReason.NOT_RECURRING) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (exception.getReason() == MatchUpdateFailureReason.CAPACITY_BELOW_CONFIRMED) {
                bindingResult.rejectValue(
                        "maxPlayers",
                        "match.update.error.capacityBelowConfirmed",
                        exception.getMessage());
            } else if (exception.getReason() == MatchUpdateFailureReason.CAPACITY_ABOVE_MAX) {
                bindingResult.rejectValue(
                        "maxPlayers",
                        "match.update.error.capacityAboveMax",
                        exception.getMessage());
            } else if (exception.getReason() == MatchUpdateFailureReason.NOT_EDITABLE) {
                return hostFormView(createEventForm, exception.getMessage(), locale, formConfig);
            } else if (exception.getReason() == MatchUpdateFailureReason.INVALID_SCHEDULE) {
                if (!isEndAfterStart(createEventForm)) {
                    bindingResult.rejectValue(
                            "endTime",
                            "match.schedule.error.endBeforeStart",
                            messageSource.getMessage(
                                    "match.schedule.error.endBeforeStart", null, locale));
                } else {
                    bindingResult.rejectValue(
                            "eventTime",
                            "match.schedule.error.startsAtPast",
                            messageSource.getMessage(
                                    "match.schedule.error.startsAtPast", null, locale));
                }
            } else {
                bindingResult.rejectValue(
                        "eventTime", "match.schedule.error.startsAtPast", exception.getMessage());
            }
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        return new ModelAndView("redirect:/matches/" + matchId + "?hostAction=seriesUpdated");
    }

    @PostMapping("/host/matches/{matchId:\\d+}/cancel")
    @PreAuthorize("@securityService.isHost(#matchId)")
    public ModelAndView cancelEvent(@PathVariable("matchId") final Long matchId) {
        final Long actingUserId = requireAuthenticatedUserId();
        findOwnedMatchOrThrowNotFound(matchId, actingUserId);
        try {
            matchService.cancelMatch(matchId, actingUserId);
        } catch (final MatchCancellationException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new ModelAndView("redirect:/matches/" + matchId + "?hostAction=cancelled");
    }

    @PostMapping("/host/matches/{matchId:\\d+}/series/cancel")
    @PreAuthorize("@securityService.isHost(#matchId)")
    public ModelAndView cancelSeries(@PathVariable("matchId") final Long matchId) {
        final Long actingUserId = requireAuthenticatedUserId();
        findOwnedEditableRecurringMatchOrThrowNotFound(matchId, actingUserId);
        try {
            matchService.cancelSeriesFromOccurrence(matchId, actingUserId);
        } catch (final MatchCancellationException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new ModelAndView("redirect:/matches/" + matchId + "?hostAction=seriesCancelled");
    }

    private ModelAndView hostFormView(
            final CreateEventForm form,
            final String formError,
            final Locale locale,
            final HostFormConfig formConfig) {
        final ModelAndView mav = new ModelAndView("host/create-match");
        mav.addObject("pageTitle", formConfig.pageTitle());
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(messageSource, locale, "/host/matches/new"));
        mav.addObject("createEventForm", form);
        mav.addObject("formError", formError);
        mav.addObject("formEyebrow", formConfig.eyebrow());
        mav.addObject("formTitle", formConfig.title());
        mav.addObject("formDescription", formConfig.description());
        mav.addObject("formAction", formConfig.action());
        mav.addObject("submitLabel", formConfig.submitLabel());
        mav.addObject("submitLoadingLabel", formConfig.submitLoadingLabel());
        mav.addObject("submitButtonId", formConfig.submitButtonId());
        mav.addObject("isEditMode", formConfig.editMode());
        mav.addObject("isSeriesEditMode", formConfig.seriesEditMode());
        mav.addObject("mapPickerEnabled", mapPickerEnabled && !mapTileUrlTemplate.isBlank());
        mav.addObject("mapTileUrlTemplate", mapTileUrlTemplate);
        mav.addObject("mapAttribution", mapAttribution);
        mav.addObject("mapDefaultLatitude", mapDefaultLatitude);
        mav.addObject("mapDefaultLongitude", mapDefaultLongitude);
        mav.addObject("mapDefaultZoom", mapDefaultZoom);
        return mav;
    }

    private Long storeBannerIfPresent(final CreateEventForm form, final Long fallbackBannerImageId)
            throws IOException {
        if (form.getBannerImage() == null || form.getBannerImage().isEmpty()) {
            return fallbackBannerImageId;
        }

        try (InputStream inputStream = form.getBannerImage().getInputStream()) {
            return imageService.store(
                    form.getBannerImage().getContentType(),
                    form.getBannerImage().getSize(),
                    inputStream);
        }
    }

    private void applyScheduleValidation(
            final CreateEventForm form, final BindingResult bindingResult, final Locale locale) {
        if (!bindingResult.hasFieldErrors("eventDate")
                && !bindingResult.hasFieldErrors("eventTime")
                && !isScheduledInFuture(form)) {
            bindingResult.rejectValue(
                    "eventTime",
                    "match.schedule.error.startsAtPast",
                    messageSource.getMessage("match.schedule.error.startsAtPast", null, locale));
        }
        if (!bindingResult.hasFieldErrors("eventDate")
                && !bindingResult.hasFieldErrors("eventTime")
                && !bindingResult.hasFieldErrors("endDate")
                && !bindingResult.hasFieldErrors("endTime")
                && !isEndAfterStart(form)) {
            bindingResult.rejectValue(
                    "endTime",
                    "match.schedule.error.endBeforeStart",
                    messageSource.getMessage("match.schedule.error.endBeforeStart", null, locale));
        }
    }

    private boolean isScheduledInFuture(final CreateEventForm form) {
        final Instant startsAt = toInstant(form.getEventDate(), form.getEventTime(), form.getTz());
        return startsAt.isAfter(Instant.now(clock));
    }

    private boolean isEndAfterStart(final CreateEventForm form) {
        final Instant startsAt = toInstant(form.getEventDate(), form.getEventTime(), form.getTz());
        final Instant endsAt = toInstant(form.getEndDate(), form.getEndTime(), form.getTz());
        return endsAt.isAfter(startsAt);
    }

    private HostFormConfig createFormConfig(final Locale locale) {
        return new HostFormConfig(
                messageSource.getMessage("page.title.hostMode", null, locale),
                messageSource.getMessage("host.eyebrow", null, locale),
                messageSource.getMessage("host.title", null, locale),
                messageSource.getMessage("host.description", null, locale),
                "/host/matches/new",
                messageSource.getMessage("host.form.submit", null, locale),
                messageSource.getMessage("host.form.submitting", null, locale),
                "publish-match-button",
                false,
                false);
    }

    private HostFormConfig editFormConfig(final Match match, final Locale locale) {
        return new HostFormConfig(
                messageSource.getMessage(
                        "page.title.hostEditMode", new Object[] {match.getTitle()}, locale),
                messageSource.getMessage("host.edit.eyebrow", null, locale),
                messageSource.getMessage("host.edit.title", null, locale),
                messageSource.getMessage("host.edit.description", null, locale),
                "/host/matches/" + match.getId() + "/edit",
                messageSource.getMessage("host.edit.form.submit", null, locale),
                messageSource.getMessage("host.edit.form.submitting", null, locale),
                "update-match-button",
                true,
                false);
    }

    private HostFormConfig seriesEditFormConfig(final Match match, final Locale locale) {
        return new HostFormConfig(
                messageSource.getMessage(
                        "page.title.hostEditMode", new Object[] {match.getTitle()}, locale),
                messageSource.getMessage("host.seriesEdit.eyebrow", null, locale),
                messageSource.getMessage("host.seriesEdit.title", null, locale),
                messageSource.getMessage("host.seriesEdit.description", null, locale),
                "/host/matches/" + match.getId() + "/series/edit",
                messageSource.getMessage("host.seriesEdit.form.submit", null, locale),
                messageSource.getMessage("host.seriesEdit.form.submitting", null, locale),
                "update-series-button",
                true,
                true);
    }

    private CreateEventForm toForm(final Match match) {
        final CreateEventForm form = new CreateEventForm();
        final LocalDateTime startsAt =
                LocalDateTime.ofInstant(match.getStartsAt(), ZoneId.systemDefault());
        form.setTitle(match.getTitle());
        form.setDescription(match.getDescription());
        form.setAddress(match.getAddress());
        form.setLatitude(match.getLatitude() == null ? "" : match.getLatitude().toString());
        form.setLongitude(match.getLongitude() == null ? "" : match.getLongitude().toString());
        form.setSport(match.getSport().getDbValue());
        form.setVisibility(match.getVisibility().getValue());
        form.setJoinPolicy(match.getJoinPolicy().getValue());
        form.setEventDate(startsAt.toLocalDate());
        form.setEventTime(startsAt.toLocalTime());
        final LocalDateTime endsAt =
                LocalDateTime.ofInstant(resolveEndsAt(match), ZoneId.systemDefault());
        form.setEndDate(endsAt.toLocalDate());
        form.setEndTime(endsAt.toLocalTime());
        form.setMaxPlayers(match.getMaxPlayers());
        form.setPricePerPlayer(match.getPricePerPlayer());
        form.setTz(ZoneId.systemDefault().getId());
        return form;
    }

    private UpdateMatchRequest toUpdateRequest(
            final CreateEventForm form, final EventStatus status, final Long bannerImageId) {
        final EventVisibility visibility =
                EventVisibility.fromDbValue(normalize(form.getVisibility())).orElse(null);
        final EventJoinPolicy joinPolicy =
                EventJoinPolicy.fromDbValue(normalize(form.getJoinPolicy())).orElse(null);
        return new UpdateMatchRequest(
                form.getAddress(),
                form.getTitle(),
                form.getDescription(),
                toInstant(form.getEventDate(), form.getEventTime(), form.getTz()),
                toInstant(form.getEndDate(), form.getEndTime(), form.getTz()),
                form.getMaxPlayers().intValue(),
                form.getPricePerPlayer(),
                Sport.fromDbValue(form.getSport()).orElse(Sport.PADEL),
                visibility,
                joinPolicy,
                status,
                bannerImageId,
                parseCoordinate(form.getLatitude()),
                parseCoordinate(form.getLongitude()));
    }

    private Instant resolveEndsAt(final Match match) {
        if (match.getEndsAt() != null) {
            return match.getEndsAt();
        }
        return match.getStartsAt().plus(Duration.ofMinutes(90));
    }

    private Match findOwnedMatchOrThrowNotFound(final Long matchId, final Long actingUserId) {
        final Match match =
                matchService
                        .findMatchById(matchId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!match.getHostUserId().equals(actingUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return match;
    }

    private Match findOwnedEditableMatchOrThrowNotFound(
            final Long matchId, final Long actingUserId) {
        final Match match = findOwnedMatchOrThrowNotFound(matchId, actingUserId);
        final EventStatus status = match.getStatus();
        if (status == EventStatus.COMPLETED || status == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return match;
    }

    private Match findOwnedEditableRecurringMatchOrThrowNotFound(
            final Long matchId, final Long actingUserId) {
        final Match match = findOwnedEditableMatchOrThrowNotFound(matchId, actingUserId);
        if (!match.isRecurringOccurrence()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return match;
    }

    private static Instant toInstant(
            final java.time.LocalDate eventDate,
            final java.time.LocalTime eventTime,
            final String timezone) {
        return eventDate.atTime(eventTime).atZone(resolveZoneId(timezone)).toInstant();
    }

    private static ZoneId resolveZoneId(final String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }

        try {
            return ZoneId.of(timezone);
        } catch (final Exception ignored) {
            return ZoneId.systemDefault();
        }
    }

    private static CreateRecurrenceRequest toRecurrenceRequest(final CreateEventForm form) {
        if (!form.isRecurring()) {
            return null;
        }

        final RecurrenceFrequency frequency =
                RecurrenceFrequency.fromValue(form.getRecurrenceFrequency()).orElseThrow();
        final RecurrenceEndMode endMode =
                RecurrenceEndMode.fromValue(form.getRecurrenceEndMode()).orElseThrow();
        return new CreateRecurrenceRequest(
                frequency,
                endMode,
                endMode == RecurrenceEndMode.UNTIL_DATE ? form.getRecurrenceUntilDate() : null,
                endMode == RecurrenceEndMode.OCCURRENCE_COUNT
                        ? form.getRecurrenceOccurrenceCount()
                        : null,
                resolveZoneId(form.getTz()));
    }

    private void validateVisibilityAndJoinPolicy(
            final CreateEventForm form, final BindingResult bindingResult, final Locale locale) {
        if (bindingResult.hasFieldErrors("visibility")) {
            return;
        }

        final String normalizedVisibility = normalize(form.getVisibility());
        final EventVisibility visibility =
                EventVisibility.fromDbValue(normalizedVisibility).orElse(null);

        if (visibility == null) {
            bindingResult.rejectValue(
                    "visibility",
                    "host.validation.visibility.invalid",
                    messageSource.getMessage("host.validation.visibility.invalid", null, locale));
            return;
        }

        if (EventVisibility.PRIVATE == visibility) {
            // Private events are always invite_only; no join policy selection needed.
            return;
        }

        if (bindingResult.hasFieldErrors("joinPolicy")) {
            return;
        }

        final String normalizedJoinPolicy = normalize(form.getJoinPolicy());

        if (normalizedJoinPolicy.isEmpty()) {
            bindingResult.rejectValue("joinPolicy", "host.validation.joinPolicy.required");
            return;
        }

        final boolean validJoinPolicy =
                EventJoinPolicy.fromDbValue(normalizedJoinPolicy).isPresent();

        if (!validJoinPolicy) {
            bindingResult.rejectValue("joinPolicy", "host.validation.joinPolicy.invalid");
        }
    }

    private void validateCoordinates(
            final CreateEventForm form, final BindingResult bindingResult, final Locale locale) {
        final String latitude = normalizeBlank(form.getLatitude());
        final String longitude = normalizeBlank(form.getLongitude());
        final boolean hasLatitude = !latitude.isEmpty();
        final boolean hasLongitude = !longitude.isEmpty();

        if (hasLatitude != hasLongitude) {
            bindingResult.rejectValue(
                    hasLatitude ? "longitude" : "latitude",
                    "CreateEventForm.coordinates.Pair",
                    messageSource.getMessage("CreateEventForm.coordinates.Pair", null, locale));
            return;
        }
        if (!hasLatitude) {
            return;
        }

        final Double parsedLatitude = parseCoordinate(latitude);
        final Double parsedLongitude = parseCoordinate(longitude);
        if (parsedLatitude == null || parsedLatitude < -90 || parsedLatitude > 90) {
            bindingResult.rejectValue(
                    "latitude",
                    "CreateEventForm.coordinates.Invalid",
                    messageSource.getMessage("CreateEventForm.coordinates.Invalid", null, locale));
        }
        if (parsedLongitude == null || parsedLongitude < -180 || parsedLongitude > 180) {
            bindingResult.rejectValue(
                    "longitude",
                    "CreateEventForm.coordinates.Invalid",
                    messageSource.getMessage("CreateEventForm.coordinates.Invalid", null, locale));
        }
    }

    private static Double parseCoordinate(final String value) {
        final String normalized = normalizeBlank(value);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(normalized);
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    private static String normalizeBlank(final String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record HostFormConfig(
            String pageTitle,
            String eyebrow,
            String title,
            String description,
            String action,
            String submitLabel,
            String submitLoadingLabel,
            String submitButtonId,
            boolean editMode,
            boolean seriesEditMode) {}
}
