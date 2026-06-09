package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.MatchNotRecurringException;
import ar.edu.itba.paw.models.exceptions.matchUpdate.MatchUpdateCapacityAboveMaxException;
import ar.edu.itba.paw.models.exceptions.matchUpdate.MatchUpdateCapacityBelowConfirmedException;
import ar.edu.itba.paw.models.exceptions.matchUpdate.MatchUpdatePendingRequestsExceedAvailableException;
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
import ar.edu.itba.paw.webapp.form.CreateEventForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final boolean mapPickerEnabled;
    private final String mapTileUrlTemplate;
    private final String mapAttribution;
    private final double mapDefaultLatitude;
    private final double mapDefaultLongitude;
    private final int mapDefaultZoom;

    @Autowired
    public HostController(
            final MatchService matchService,
            @Value("${map.picker.enabled:false}") final boolean mapPickerEnabled,
            @Value("${map.tiles.urlTemplate:}") final String mapTileUrlTemplate,
            @Value("${map.tiles.attribution:}") final String mapAttribution,
            @Value("${map.default.latitude:" + DEFAULT_MAP_LATITUDE + "}")
                    final double mapDefaultLatitude,
            @Value("${map.default.longitude:" + DEFAULT_MAP_LONGITUDE + "}")
                    final double mapDefaultLongitude,
            @Value("${map.default.zoom:" + DEFAULT_MAP_ZOOM + "}") final int mapDefaultZoom) {
        this.matchService = matchService;
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
    public ModelAndView redirectLegacyCreateEvent() {
        return new ModelAndView("redirect:/matches/new");
    }

    @GetMapping("/matches/new")
    public ModelAndView showCreateEvent(final Locale locale) {
        return hostFormView(createEventForm(), null, locale, createFormConfig(locale));
    }

    @PostMapping({"/matches/new", "/host/matches/new"})
    public ModelAndView publishEvent(
            @AuthenticatedUser final User user,
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale) {

        final HostFormConfig formConfig = createFormConfig(locale);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        final CreateMatchRequest request =
                new CreateMatchRequest(
                        user,
                        createEventForm.getAddress(),
                        createEventForm.getLatitude(),
                        createEventForm.getLongitude(),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        createEventForm.getEventDate(),
                        createEventForm.getEventTime(),
                        createEventForm.getEndDate(),
                        createEventForm.getEndTime(),
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

        final Match createdMatch = matchService.createMatch(request);
        return new ModelAndView("redirect:/matches/" + createdMatch.getId());
    }

    @GetMapping("/host/matches/{matchId:\\d+}/edit")
    public ModelAndView showEditEvent(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final Locale locale) {
        final Match match = matchService.findMatchById(matchId).orElse(null);
        return hostFormView(toForm(match), null, locale, editFormConfig(match, locale));
    }

    @PostMapping("/host/matches/{matchId:\\d+}/edit")
    public ModelAndView updateEvent(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final Match existingMatch = matchService.findMatchById(matchId).orElse(null);
        final HostFormConfig formConfig = editFormConfig(existingMatch, locale);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        final UpdateMatchRequest request =
                new UpdateMatchRequest(
                        createEventForm.getAddress(),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        createEventForm.getEventDate(),
                        createEventForm.getEventTime(),
                        createEventForm.getEndDate(),
                        createEventForm.getEndTime(),
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
            matchService.updateMatch(matchId, user, request);
            redirectAttributes.addFlashAttribute("hostAction", "updated");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchUpdateCapacityBelowConfirmedException e) {
            bindingResult.rejectValue("maxPlayers", "match.update.error." + e.getMessage());
        } catch (final MatchUpdateCapacityAboveMaxException e) {
            bindingResult.rejectValue("maxPlayers", "match.update.error." + e.getMessage());
        } catch (final MatchUpdatePendingRequestsExceedAvailableException e) {
            bindingResult.rejectValue("joinPolicy", "match.update.error." + e.getMessage());
        }
        return hostFormView(createEventForm, null, locale, formConfig);
    }

    @GetMapping("/host/matches/{matchId:\\d+}/series/edit")
    public ModelAndView showEditSeries(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final Locale locale) {
        final Match match = matchService.findMatchById(matchId).orElse(null);
        return hostFormView(toForm(match), null, locale, seriesEditFormConfig(match, locale));
    }

    @PostMapping("/host/matches/{matchId:\\d+}/series/edit")
    public ModelAndView updateSeries(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale,
            final RedirectAttributes redirectAttributes) {
        final Match match = matchService.findMatchById(matchId).orElse(null);
        final HostFormConfig formConfig = seriesEditFormConfig(match, locale);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale, formConfig);
        }

        final UpdateMatchRequest request = toUpdateRequest(createEventForm, match.getStatus());

        try {
            matchService.updateSeriesFromOccurrence(matchId, user, request);
            redirectAttributes.addFlashAttribute("hostAction", "seriesUpdated");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchUpdateCapacityBelowConfirmedException e) {
            bindingResult.rejectValue("maxPlayers", "match.update.error." + e.getMessage());
        } catch (final MatchUpdateCapacityAboveMaxException e) {
            bindingResult.rejectValue("maxPlayers", "match.update.error." + e.getMessage());
        } catch (final MatchUpdatePendingRequestsExceedAvailableException e) {
            bindingResult.rejectValue("joinPolicy", "match.update.error." + e.getMessage());
        }
        return hostFormView(createEventForm, null, locale, formConfig);
    }

    @PostMapping("/host/matches/{matchId:\\d+}/cancel")
    public ModelAndView cancelEvent(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        matchService.cancelMatch(matchId, user);
        redirectAttributes.addFlashAttribute("hostAction", "cancelled");
        return new ModelAndView("redirect:/matches/" + matchId);
    }

    @PostMapping("/host/matches/{matchId:\\d+}/series/cancel")
    public ModelAndView cancelSeries(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {

        try {
            matchService.cancelSeriesFromOccurrence(matchId, user);
        } catch (final MatchNotRecurringException exception) {
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
        mav.addObject("pageTitleCode", formConfig.pageTitleCode());
        mav.addObject("pageTitleArgument", formConfig.pageTitleArgument());
        mav.addObject("createEventForm", form);
        mav.addObject("formError", formError);
        mav.addObject("formEyebrowCode", formConfig.eyebrowCode());
        mav.addObject("formTitleCode", formConfig.titleCode());
        mav.addObject("formDescriptionCode", formConfig.descriptionCode());
        mav.addObject("formAction", formConfig.action());
        mav.addObject("submitLabelCode", formConfig.submitLabelCode());
        mav.addObject("submitLoadingLabelCode", formConfig.submitLoadingLabelCode());
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
        final EventVisibility visibility =
                form.getVisibility() == null ? EventVisibility.PUBLIC : form.getVisibility();
        final EventJoinPolicy joinPolicy =
                form.getJoinPolicy() == null ? EventJoinPolicy.INVITE_ONLY : form.getJoinPolicy();
        mav.addObject("visibilityKey", "host.form.visibility." + visibility.getDbValue());
        mav.addObject("joinPolicyKey", "host.form.joinPolicy." + joinPolicy.getDbValue());
        return mav;
    }

    private HostFormConfig createFormConfig(final Locale locale) {
        return new HostFormConfig(
                "page.title.hostMode",
                null,
                "host.eyebrow",
                "host.title",
                "host.description",
                "/matches/new",
                "host.form.submit",
                "host.form.submitting",
                "publish-match-button",
                null,
                false,
                false);
    }

    private HostFormConfig editFormConfig(final Match match, final Locale locale) {
        return new HostFormConfig(
                "page.title.hostEditMode",
                match.getTitle(),
                "host.edit.eyebrow",
                "host.edit.title",
                "host.edit.description",
                "/host/matches/" + match.getId() + "/edit",
                "host.edit.form.submit",
                "host.edit.form.submitting",
                "update-match-button",
                bannerUrlFor(match),
                true,
                false);
    }

    private HostFormConfig seriesEditFormConfig(final Match match, final Locale locale) {
        return new HostFormConfig(
                "page.title.hostEditMode",
                match.getTitle(),
                "host.seriesEdit.eyebrow",
                "host.seriesEdit.title",
                "host.seriesEdit.description",
                "/host/matches/" + match.getId() + "/series/edit",
                "host.seriesEdit.form.submit",
                "host.seriesEdit.form.submitting",
                "update-series-button",
                bannerUrlFor(match),
                true,
                true);
    }

    private CreateEventForm toForm(final Match match) {
        final CreateEventForm form = new CreateEventForm();
        final OffsetDateTime startsAt = match.getStartsAtDateTime();
        form.setTitle(match.getTitle());
        form.setDescription(match.getDescription());
        form.setAddress(match.getAddress());
        form.setLatitude(match.getLatitude());
        form.setLongitude(match.getLongitude());
        form.setSport(match.getSport());
        form.setVisibility(formVisibility(match));
        form.setJoinPolicy(formJoinPolicy(match));
        form.setEventDate(startsAt.toLocalDate());
        form.setEventTime(startsAt.toLocalTime());
        final OffsetDateTime endsAt = PlatformTime.toOffsetDateTime(resolveEndsAt(match));
        form.setEndDate(endsAt.toLocalDate());
        form.setEndTime(endsAt.toLocalTime());
        form.setMaxPlayers(match.getMaxPlayers());
        form.setPricePerPlayer(match.getPricePerPlayer());
        return form;
    }

    private static EventVisibility formVisibility(final Match match) {
        return match.getVisibility() == EventVisibility.INVITE_ONLY
                ? EventVisibility.PRIVATE
                : match.getVisibility();
    }

    private static EventJoinPolicy formJoinPolicy(final Match match) {
        return match.getJoinPolicy() == null ? EventJoinPolicy.INVITE_ONLY : match.getJoinPolicy();
    }

    private UpdateMatchRequest toUpdateRequest(
            final CreateEventForm form, final EventStatus status) {
        final EventVisibility visibility = form.getVisibility();
        final EventJoinPolicy joinPolicy = form.getJoinPolicy();
        return new UpdateMatchRequest(
                form.getAddress(),
                form.getTitle(),
                form.getDescription(),
                form.getEventDate(),
                form.getEventTime(),
                form.getEndDate(),
                form.getEndTime(),
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
                        : null);
    }

    private ImageUpload bannerUpload(
            final MultipartFile
                    bannerImage) { // TODO: move this to a different file. It's also used in other
        // controllers
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
            public String getOriginalFilename() {
                return bannerImage.getOriginalFilename();
            }

            @Override
            public java.io.InputStream getContentStream() throws java.io.IOException {
                return bannerImage.getInputStream();
            }
        };
    }

    private record HostFormConfig(
            String pageTitleCode,
            String pageTitleArgument,
            String eyebrowCode,
            String titleCode,
            String descriptionCode,
            String action,
            String submitLabelCode,
            String submitLoadingLabelCode,
            String submitButtonId,
            String bannerImageUrl,
            boolean editMode,
            boolean seriesEditMode) {}
}
