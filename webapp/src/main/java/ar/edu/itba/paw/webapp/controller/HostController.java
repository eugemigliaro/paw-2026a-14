package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.MatchUpdateFailureReason;
import ar.edu.itba.paw.services.UpdateMatchRequest;
import ar.edu.itba.paw.services.exceptions.MatchCancellationException;
import ar.edu.itba.paw.services.exceptions.MatchUpdateException;
import ar.edu.itba.paw.webapp.form.CreateEventForm;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
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
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HostController {

    private final MatchService matchService;
    private final ImageService imageService;
    private final Clock clock;
    private final MessageSource messageSource;

    @Autowired
    public HostController(
            final MatchService matchService,
            final ImageService imageService,
            final Clock clock,
            final MessageSource messageSource) {
        this.matchService = matchService;
        this.imageService = imageService;
        this.clock = clock;
        this.messageSource = messageSource;
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

        final CreateMatchRequest request =
                new CreateMatchRequest(
                        actingUserId,
                        createEventForm.getAddress(),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        startsAt,
                        endsAt,
                        createEventForm.getMaxPlayers(),
                        createEventForm.getPricePerPlayer(),
                        Sport.fromDbValue(createEventForm.getSport()).orElse(Sport.PADEL),
                        "public",
                        "open",
                        bannerImageId);

        return new ModelAndView("redirect:/matches/" + matchService.createMatch(request).getId());
    }

    @GetMapping("/host/matches/{matchId}/edit")
    public ModelAndView showEditEvent(
            @PathVariable("matchId") final String matchId, final Locale locale) {
        final Long parsedMatchId = parseMatchIdOrThrowNotFound(matchId);
        final Long actingUserId = requireAuthenticatedUserId();
        final Match match = findOwnedEditableMatchOrThrowNotFound(parsedMatchId, actingUserId);
        return hostFormView(toForm(match), null, locale, editFormConfig(match, locale));
    }

    @PostMapping("/host/matches/{matchId}/edit")
    public ModelAndView updateEvent(
            @PathVariable("matchId") final String matchId,
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale) {
        final Long parsedMatchId = parseMatchIdOrThrowNotFound(matchId);
        final Long actingUserId = requireAuthenticatedUserId();
        final Match existingMatch =
                findOwnedEditableMatchOrThrowNotFound(parsedMatchId, actingUserId);
        final HostFormConfig formConfig = editFormConfig(existingMatch, locale);
        applyScheduleValidation(createEventForm, bindingResult, locale);

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

        final UpdateMatchRequest request =
                new UpdateMatchRequest(
                        createEventForm.getAddress(),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        startsAt,
                        endsAt,
                        createEventForm.getMaxPlayers(),
                        createEventForm.getPricePerPlayer(),
                        Sport.fromDbValue(createEventForm.getSport()).orElse(Sport.PADEL),
                        existingMatch.getVisibility(),
                        existingMatch.getStatus(),
                        bannerImageId);

        try {
            matchService.updateMatch(parsedMatchId, actingUserId, request);
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

        return new ModelAndView("redirect:/matches/" + parsedMatchId + "?hostAction=updated");
    }

    @PostMapping("/host/matches/{matchId}/cancel")
    public ModelAndView cancelEvent(@PathVariable("matchId") final String matchId) {
        final Long parsedMatchId = parseMatchIdOrThrowNotFound(matchId);
        final Long actingUserId = requireAuthenticatedUserId();
        findOwnedMatchOrThrowNotFound(parsedMatchId, actingUserId);
        try {
            matchService.cancelMatch(parsedMatchId, actingUserId);
        } catch (final MatchCancellationException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new ModelAndView("redirect:/matches/" + parsedMatchId + "?hostAction=cancelled");
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
                ShellViewModelFactory.hostShell(messageSource, locale, "/host/matches/new"));
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
                true);
    }

    private CreateEventForm toForm(final Match match) {
        final CreateEventForm form = new CreateEventForm();
        final LocalDateTime startsAt =
                LocalDateTime.ofInstant(match.getStartsAt(), ZoneId.systemDefault());
        form.setTitle(match.getTitle());
        form.setDescription(match.getDescription());
        form.setAddress(match.getAddress());
        form.setSport(match.getSport().getDbValue());
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
        final EventStatus status = EventStatus.fromDbValue(match.getStatus()).orElse(null);
        if (status == EventStatus.COMPLETED || status == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return match;
    }

    private Long requireAuthenticatedUserId() {
        return CurrentAuthenticatedUser.get()
                .map(AuthenticatedUserPrincipal::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private static Long parseMatchIdOrThrowNotFound(final String matchId) {
        if (matchId == null || !matchId.matches("\\d+")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        try {
            return Long.valueOf(matchId);
        } catch (final NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
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

    private record HostFormConfig(
            String pageTitle,
            String eyebrow,
            String title,
            String description,
            String action,
            String submitLabel,
            String submitLoadingLabel,
            String submitButtonId,
            boolean editMode) {}
}
