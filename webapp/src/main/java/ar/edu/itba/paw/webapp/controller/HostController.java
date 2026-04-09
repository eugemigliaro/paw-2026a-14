package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.ActionVerificationService;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.webapp.form.CreateEventForm;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HostController {

    private final ActionVerificationService actionVerificationService;
    private final ImageService imageService;
    private final Clock clock;

    @Autowired
    public HostController(
            final ActionVerificationService actionVerificationService,
            final ImageService imageService,
            final Clock clock) {
        this.actionVerificationService = actionVerificationService;
        this.imageService = imageService;
        this.clock = clock;
    }

    @ModelAttribute("createEventForm")
    public CreateEventForm createEventForm() {
        return new CreateEventForm();
    }

    @GetMapping("/host/events/new")
    public ModelAndView showCreateEvent() {
        final ModelAndView mav = new ModelAndView("host/create-event");
        mav.addObject("shell", ShellViewModelFactory.hostShell());
        mav.addObject("createEventForm", createEventForm());
        return mav;
    }

    @PostMapping("/host/events/new")
    public ModelAndView publishEvent(
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult) {
        if (!bindingResult.hasFieldErrors("eventDate")
                && !bindingResult.hasFieldErrors("eventTime")
                && !isScheduledInFuture(createEventForm)) {
            bindingResult.rejectValue(
                    "eventTime", "eventTime.past", "Event date and time cannot be in the past");
        }
        if (!bindingResult.hasFieldErrors("eventDate")
                && !bindingResult.hasFieldErrors("eventTime")
                && !bindingResult.hasFieldErrors("endTime")
                && !isEndTimeAfterStartTime(createEventForm)) {
            bindingResult.rejectValue(
                    "endTime", "endTime.beforeStart", "End time must be after start time");
        }

        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null);
        }

        final Instant startsAt =
                toInstant(
                        createEventForm.getEventDate(),
                        createEventForm.getEventTime(),
                        createEventForm.getTimezone());
        final Instant endsAt =
                createEventForm.getEndTime() == null
                        ? null
                        : toInstant(
                                createEventForm.getEventDate(),
                                createEventForm.getEndTime(),
                                createEventForm.getTimezone());

        final Long bannerImageId;
        try {
            bannerImageId = storeBannerIfPresent(createEventForm);
        } catch (final IllegalArgumentException exception) {
            return hostFormView(createEventForm, exception.getMessage());
        } catch (final IOException exception) {
            return hostFormView(
                    createEventForm, "We could not process the uploaded image. Please try again.");
        }

        final CreateMatchRequest request =
                new CreateMatchRequest(
                        null,
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

        try {
            final VerificationRequestResult requestResult =
                    actionVerificationService.requestMatchCreation(
                            request, createEventForm.getEmail());
            final ModelAndView mav = new ModelAndView("verification/check-email");
            mav.addObject("shell", ShellViewModelFactory.hostShell());
            mav.addObject("title", "Check your email");
            mav.addObject(
                    "summary",
                    "We sent a one-time confirmation link to "
                            + requestResult.getEmail()
                            + " so you can publish your event.");
            mav.addObject(
                    "expiresAtLabel",
                    java.time.format.DateTimeFormatter.ofLocalizedDateTime(
                                    java.time.format.FormatStyle.MEDIUM,
                                    java.time.format.FormatStyle.SHORT)
                            .withLocale(java.util.Locale.US)
                            .format(requestResult.getExpiresAt().atZone(ZoneId.systemDefault())));
            mav.addObject("backHref", "/host/events/new");
            mav.addObject("actionLabel", "Back to create event");
            mav.addObject("eyebrow", "Event publication requested");
            return mav;
        } catch (final VerificationFailureException exception) {
            return hostFormView(createEventForm, exception.getMessage());
        }
    }

    private ModelAndView hostFormView(final CreateEventForm form, final String formError) {
        final ModelAndView mav = new ModelAndView("host/create-event");
        mav.addObject("shell", ShellViewModelFactory.hostShell());
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
        final Instant startsAt =
                toInstant(form.getEventDate(), form.getEventTime(), form.getTimezone());
        return startsAt.isAfter(Instant.now(clock));
    }

    private boolean isEndTimeAfterStartTime(final CreateEventForm form) {
        if (form.getEndTime() == null) {
            return true;
        }

        final Instant startsAt =
                toInstant(form.getEventDate(), form.getEventTime(), form.getTimezone());
        final Instant endsAt =
                toInstant(form.getEventDate(), form.getEndTime(), form.getTimezone());
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
}
