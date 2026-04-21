package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.webapp.form.CreateEventForm;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HostController {

    private static final String VISIBILITY_PUBLIC = "public";
    private static final String VISIBILITY_PRIVATE = "private";
    private static final String JOIN_POLICY_DIRECT = "direct";
    private static final String JOIN_POLICY_APPROVAL_REQUIRED = "approval_required";

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
        final ModelAndView mav = new ModelAndView("host/create-match");
        mav.addObject("shell", ShellViewModelFactory.hostShell(messageSource, locale));
        mav.addObject("createEventForm", createEventForm());
        return mav;
    }

    @PostMapping("/host/matches/new")
    public ModelAndView publishEvent(
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale) {
        final AuthenticatedUserPrincipal currentUser =
                CurrentAuthenticatedUser.get()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!bindingResult.hasFieldErrors("eventDate")
                && !bindingResult.hasFieldErrors("eventTime")
                && !isScheduledInFuture(createEventForm)) {
            bindingResult.rejectValue(
                    "eventTime", "eventTime.past", "Match date and time cannot be in the past");
        }
        if (!bindingResult.hasFieldErrors("eventDate")
                && !bindingResult.hasFieldErrors("eventTime")
                && !bindingResult.hasFieldErrors("endTime")
                && !isEndTimeAfterStartTime(createEventForm)) {
            bindingResult.rejectValue(
                    "endTime", "endTime.beforeStart", "End time must be after start time");
        }
        validateVisibilityAndJoinPolicy(createEventForm, bindingResult);

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale);
        }

        final Instant startsAt =
                toInstant(
                        createEventForm.getEventDate(),
                        createEventForm.getEventTime(),
                        createEventForm.getTz());
        final Instant endsAt =
                createEventForm.getEndTime() == null
                        ? null
                        : toInstant(
                                createEventForm.getEventDate(),
                                createEventForm.getEndTime(),
                                createEventForm.getTz());

        final Long bannerImageId;
        try {
            bannerImageId = storeBannerIfPresent(createEventForm);
        } catch (final IllegalArgumentException exception) {
            return hostFormView(createEventForm, exception.getMessage(), locale);
        } catch (final IOException exception) {
            return hostFormView(
                    createEventForm,
                    messageSource.getMessage("host.imageError", null, locale),
                    locale);
        }

        final CreateMatchRequest request =
                new CreateMatchRequest(
                        currentUser.getUserId(),
                        createEventForm.getAddress(),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        startsAt,
                        endsAt,
                        createEventForm.getMaxPlayers(),
                        createEventForm.getPricePerPlayer(),
                        Sport.fromDbValue(createEventForm.getSport()).orElse(Sport.PADEL),
                        normalize(createEventForm.getVisibility()),
                        normalize(createEventForm.getJoinPolicy()),
                        "open",
                        bannerImageId);

        return new ModelAndView("redirect:/matches/" + matchService.createMatch(request).getId());
    }

    private ModelAndView hostFormView(
            final CreateEventForm form, final String formError, final Locale locale) {
        final ModelAndView mav = new ModelAndView("host/create-match");
        mav.addObject("shell", ShellViewModelFactory.hostShell(messageSource, locale));
        mav.addObject("createEventForm", form);
        mav.addObject("formError", formError);
        return mav;
    }

    private Long storeBannerIfPresent(final CreateEventForm form) throws IOException {
        if (form.getBannerImage() == null || form.getBannerImage().isEmpty()) {
            return null;
        }

        try (InputStream inputStream = form.getBannerImage().getInputStream()) {
            return imageService.store(
                    form.getBannerImage().getContentType(),
                    form.getBannerImage().getSize(),
                    inputStream);
        }
    }

    private boolean isScheduledInFuture(final CreateEventForm form) {
        final Instant startsAt = toInstant(form.getEventDate(), form.getEventTime(), form.getTz());
        return startsAt.isAfter(Instant.now(clock));
    }

    private boolean isEndTimeAfterStartTime(final CreateEventForm form) {
        if (form.getEndTime() == null) {
            return true;
        }

        final Instant startsAt = toInstant(form.getEventDate(), form.getEventTime(), form.getTz());
        final Instant endsAt = toInstant(form.getEventDate(), form.getEndTime(), form.getTz());
        return endsAt.isAfter(startsAt);
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

    private static void validateVisibilityAndJoinPolicy(
            final CreateEventForm form, final BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors("visibility")
                || bindingResult.hasFieldErrors("joinPolicy")) {
            return;
        }

        final String visibility = normalize(form.getVisibility());
        final String joinPolicy = normalize(form.getJoinPolicy());

        final boolean validVisibility =
                VISIBILITY_PUBLIC.equals(visibility) || VISIBILITY_PRIVATE.equals(visibility);
        final boolean validJoinPolicy =
                JOIN_POLICY_DIRECT.equals(joinPolicy)
                        || JOIN_POLICY_APPROVAL_REQUIRED.equals(joinPolicy);

        if (!validVisibility) {
            bindingResult.rejectValue(
                    "visibility",
                    "host.validation.visibility.invalid",
                    "Choose a valid visibility");
        }

        if (!validJoinPolicy) {
            bindingResult.rejectValue(
                    "joinPolicy",
                    "host.validation.joinPolicy.invalid",
                    "Choose a valid join policy");
        }

        if (VISIBILITY_PRIVATE.equals(visibility) && JOIN_POLICY_DIRECT.equals(joinPolicy)) {
            bindingResult.rejectValue(
                    "joinPolicy",
                    "host.validation.joinPolicy.privateRequiresApproval",
                    "Private events must be approval-required");
        }
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
