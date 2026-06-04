package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.RecurrenceEndMode;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.CreateRecurrenceRequest;
import ar.edu.itba.paw.services.ImageUpload;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UpdateMatchRequest;
import ar.edu.itba.paw.services.exceptions.matchCancelation.MatchCancellationException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateCapacityAboveMaxException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateCapacityBelowConfirmedException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateForbiddenException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateNotEditableException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateNotFoundException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateNotRecurringException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdatePendingRequestsExceedAvailableException;
import ar.edu.itba.paw.webapp.form.CreateEventForm;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HostController {

    private static final double DEFAULT_MAP_LATITUDE = -34.6037;
    private static final double DEFAULT_MAP_LONGITUDE = -58.3816;
    private static final int DEFAULT_MAP_ZOOM = 14;

    private final MatchService matchService;
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
        this.messageSource = messageSource;
        this.mapPickerEnabled = mapPickerEnabled;
        this.mapTileUrlTemplate = mapTileUrlTemplate == null ? "" : mapTileUrlTemplate;
        this.mapAttribution = mapAttribution == null ? "" : mapAttribution;
        this.mapDefaultLatitude = mapDefaultLatitude;
        this.mapDefaultLongitude = mapDefaultLongitude;
        this.mapDefaultZoom = mapDefaultZoom;
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

        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final HostFormConfig formConfig = createFormConfig(locale);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        final Instant startsAt =
                toInstant(
                        createEventForm.getEventDate(),
                        createEventForm.getEventTime(),
                        createEventForm.getTimezone());
        final Instant endsAt =
                toInstant(
                        createEventForm.getEndDate(),
                        createEventForm.getEndTime(),
                        createEventForm.getTimezone());

        final CreateMatchRequest request =
                new CreateMatchRequest(
                        actingUser,
                        createEventForm.getAddress(),
                        createEventForm.getLatitude(),
                        createEventForm.getLongitude(),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        startsAt,
                        endsAt,
                        createEventForm.getMaxPlayers(),
                        createEventForm.getPricePerPlayer(),
                        createEventForm.getSport() == null
                                ? Sport.PADEL
                                : createEventForm.getSport(),
                        createEventForm.getVisibility(),
                        createEventForm.getJoinPolicy(),
                        EventStatus.OPEN,
                        bannerUpload(createEventForm.getBannerImage()),
                        toRecurrenceRequest(createEventForm));

        try {
            final Match createdMatch = matchService.createMatch(request);
            return new ModelAndView("redirect:/matches/" + createdMatch.getId());
        } catch (final IllegalArgumentException exception) {
            return hostFormView(createEventForm, exception.getMessage(), locale, formConfig);
        }
    }

    @GetMapping("/host/matches/{matchId:\\d+}/edit")
    public ModelAndView showEditEvent(
            @PathVariable("matchId") final Long matchId, final Locale locale) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final Match match = findEditableMatchOrThrowNotFound(matchId, actingUser);
        return hostFormView(toForm(match), null, locale, editFormConfig(match, locale));
    }

    @PostMapping("/host/matches/{matchId:\\d+}/edit")
    public ModelAndView updateEvent(
            @PathVariable("matchId") final Long matchId,
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final Match existingMatch = findEditableMatchOrThrowNotFound(matchId, actingUser);
        final HostFormConfig formConfig = editFormConfig(existingMatch, locale);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        final Instant startsAt =
                toInstant(
                        createEventForm.getEventDate(),
                        createEventForm.getEventTime(),
                        createEventForm.getTimezone());
        final Instant endsAt =
                toInstant(
                        createEventForm.getEndDate(),
                        createEventForm.getEndTime(),
                        createEventForm.getTimezone());

        final UpdateMatchRequest request =
                new UpdateMatchRequest(
                        createEventForm.getAddress(),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        startsAt,
                        endsAt,
                        createEventForm.getMaxPlayers().intValue(),
                        createEventForm.getPricePerPlayer(),
                        createEventForm.getSport() == null
                                ? Sport.PADEL
                                : createEventForm.getSport(),
                        createEventForm.getVisibility(),
                        createEventForm.getJoinPolicy(),
                        existingMatch.getStatus(),
                        bannerUpload(createEventForm.getBannerImage()),
                        createEventForm.getLatitude(),
                        createEventForm.getLongitude());

        try {
            matchService.updateMatch(matchId, actingUser, request);
        } catch (final MatchUpdateNotFoundException | MatchUpdateForbiddenException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (final MatchUpdateCapacityBelowConfirmedException exception) {
            bindingResult.rejectValue("maxPlayers", "match.update.error.capacityBelowConfirmed");
            return hostFormView(createEventForm, null, locale, formConfig);
        } catch (final MatchUpdateCapacityAboveMaxException exception) {
            bindingResult.rejectValue("maxPlayers", "match.update.error.capacityAboveMax");
            return hostFormView(createEventForm, null, locale, formConfig);
        } catch (final MatchUpdatePendingRequestsExceedAvailableException exception) {
            bindingResult.rejectValue(
                    "joinPolicy", "match.update.error.pendingRequestsExceedAvailable");
            return hostFormView(createEventForm, null, locale, formConfig);
        } catch (final MatchUpdateNotEditableException exception) {
            return hostFormView(
                    createEventForm,
                    messageSource.getMessage("match.update.error.notEditable", null, locale),
                    locale,
                    formConfig);
        }

        redirectAttributes.addFlashAttribute("hostAction", "updated");
        return new ModelAndView("redirect:/matches/" + matchId);
    }

    @GetMapping("/host/matches/{matchId:\\d+}/series/edit")
    public ModelAndView showEditSeries(
            @PathVariable("matchId") final Long matchId, final Locale locale) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final Match match = findEditableRecurringMatchOrThrowNotFound(matchId, actingUser);
        return hostFormView(toForm(match), null, locale, seriesEditFormConfig(match, locale));
    }

    @PostMapping("/host/matches/{matchId:\\d+}/series/edit")
    public ModelAndView updateSeries(
            @PathVariable("matchId") final Long matchId,
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        final Match match = findEditableRecurringMatchOrThrowNotFound(matchId, actingUser);
        final HostFormConfig formConfig = seriesEditFormConfig(match, locale);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        final UpdateMatchRequest request = toUpdateRequest(createEventForm, match.getStatus());

        try {
            matchService.updateSeriesFromOccurrence(matchId, actingUser, request);
        } catch (final MatchUpdateNotFoundException
                | MatchUpdateForbiddenException
                | MatchUpdateNotRecurringException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (final MatchUpdateCapacityBelowConfirmedException exception) {
            bindingResult.rejectValue("maxPlayers", "match.update.error.capacityBelowConfirmed");
            return hostFormView(createEventForm, null, locale, formConfig);
        } catch (final MatchUpdateCapacityAboveMaxException exception) {
            bindingResult.rejectValue("maxPlayers", "match.update.error.capacityAboveMax");
            return hostFormView(createEventForm, null, locale, formConfig);
        } catch (final MatchUpdatePendingRequestsExceedAvailableException exception) {
            bindingResult.rejectValue(
                    "joinPolicy", "match.update.error.pendingRequestsExceedAvailable");
            return hostFormView(createEventForm, null, locale, formConfig);
        } catch (final MatchUpdateNotEditableException exception) {
            return hostFormView(
                    createEventForm,
                    messageSource.getMessage("match.update.error.notEditable", null, locale),
                    locale,
                    formConfig);
        }

        redirectAttributes.addFlashAttribute("hostAction", "seriesUpdated");
        return new ModelAndView("redirect:/matches/" + matchId);
    }

    @PostMapping("/host/matches/{matchId:\\d+}/cancel")
    public ModelAndView cancelEvent(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        try {
            matchService.cancelMatch(matchId, actingUser);
        } catch (final MatchCancellationException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirectAttributes.addFlashAttribute("hostAction", "cancelled");
        return new ModelAndView("redirect:/matches/" + matchId);
    }

    @PostMapping("/host/matches/{matchId:\\d+}/series/cancel")
    public ModelAndView cancelSeries(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {

        final User actingUser = SecurityControllerUtils.requireAuthenticatedUser();
        try {
            matchService.cancelSeriesFromOccurrence(matchId, actingUser);
        } catch (final MatchCancellationException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirectAttributes.addFlashAttribute("hostAction", "seriesCancelled");
        return new ModelAndView("redirect:/matches/" + matchId);
    }

    private ModelAndView hostFormView(
            final CreateEventForm form,
            final String formError,
            final Locale locale,
            final HostFormConfig formConfig) {
        final ModelAndView mav = new ModelAndView("host/create-match");
        mav.addObject("pageTitle", formConfig.pageTitle());
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
        mav.addObject("currentBannerImageUrl", formConfig.bannerImageUrl());
        mav.addObject("mapPickerEnabled", mapPickerEnabled && !mapTileUrlTemplate.isBlank());
        mav.addObject("mapTileUrlTemplate", mapTileUrlTemplate);
        mav.addObject("mapAttribution", mapAttribution);
        mav.addObject("mapDefaultLatitude", mapDefaultLatitude);
        mav.addObject("mapDefaultLongitude", mapDefaultLongitude);
        mav.addObject("mapDefaultZoom", mapDefaultZoom);
        return mav;
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
                null,
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
                bannerUrlFor(match),
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
                bannerUrlFor(match),
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
        form.setLatitude(match.getLatitude());
        form.setLongitude(match.getLongitude());
        form.setSport(match.getSport());
        form.setVisibility(match.getVisibility());
        form.setJoinPolicy(match.getJoinPolicy());
        form.setEventDate(startsAt.toLocalDate());
        form.setEventTime(startsAt.toLocalTime());
        final LocalDateTime endsAt =
                LocalDateTime.ofInstant(resolveEndsAt(match), ZoneId.systemDefault());
        form.setEndDate(endsAt.toLocalDate());
        form.setEndTime(endsAt.toLocalTime());
        form.setMaxPlayers(match.getMaxPlayers());
        form.setPricePerPlayer(match.getPricePerPlayer());
        form.setTimezone(ZoneId.systemDefault());
        return form;
    }

    private UpdateMatchRequest toUpdateRequest(
            final CreateEventForm form, final EventStatus status) {
        final EventVisibility visibility = form.getVisibility();
        final EventJoinPolicy joinPolicy = form.getJoinPolicy();
        return new UpdateMatchRequest(
                form.getAddress(),
                form.getTitle(),
                form.getDescription(),
                toInstant(form.getEventDate(), form.getEventTime(), form.getTimezone()),
                toInstant(form.getEndDate(), form.getEndTime(), form.getTimezone()),
                form.getMaxPlayers().intValue(),
                form.getPricePerPlayer(),
                form.getSport() == null ? Sport.PADEL : form.getSport(),
                visibility,
                joinPolicy,
                status,
                bannerUpload(form.getBannerImage()),
                form.getLatitude(),
                form.getLongitude());
    }

    private Instant resolveEndsAt(final Match match) {
        if (match.getEndsAt() != null) {
            return match.getEndsAt();
        }
        return match.getStartsAt().plus(Duration.ofMinutes(90));
    }

    private Match findEditableMatchOrThrowNotFound(final Long matchId, final User actingUser) {
        try {
            return matchService.findEditableMatchForHost(matchId, actingUser);
        } catch (final MatchUpdateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private Match findEditableRecurringMatchOrThrowNotFound(
            final Long matchId, final User actingUser) {
        try {
            return matchService.findEditableRecurringMatchForHost(matchId, actingUser);
        } catch (final MatchUpdateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static Instant toInstant(
            final LocalDate eventDate, final LocalTime eventTime, final ZoneId timezone) {
        return eventDate
                .atTime(eventTime)
                .atZone(timezone == null ? ZoneId.systemDefault() : timezone)
                .toInstant();
    }

    private static CreateRecurrenceRequest toRecurrenceRequest(final CreateEventForm form) {
        if (!form.isRecurring()) {
            return null;
        }

        return new CreateRecurrenceRequest(
                form.getRecurrenceFrequency(),
                form.getRecurrenceEndMode(),
                form.getRecurrenceEndMode() == RecurrenceEndMode.UNTIL_DATE
                        ? form.getRecurrenceUntilDate()
                        : null,
                form.getRecurrenceEndMode() == RecurrenceEndMode.OCCURRENCE_COUNT
                        ? form.getRecurrenceOccurrenceCount()
                        : null,
                form.getTimezone());
    }

    private ImageUpload bannerUpload(final MultipartFile bannerImage) {
        if (bannerImage == null) {
            return null;
        }
        return new ImageUpload() {
            @Override
            public String getContentType() {
                return bannerImage.getContentType();
            }

            @Override
            public long getContentLength() {
                return bannerImage.getSize();
            }

            @Override
            public java.io.InputStream getContentStream() throws java.io.IOException {
                return bannerImage.getInputStream();
            }
        };
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
            String bannerImageUrl,
            boolean editMode,
            boolean seriesEditMode) {}
}
