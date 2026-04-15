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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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
    private final MessageSource messageSource;

    @Autowired
    public HostController(
            final ActionVerificationService actionVerificationService,
            final ImageService imageService,
            final MessageSource messageSource) {
        this.actionVerificationService = actionVerificationService;
        this.imageService = imageService;
        this.messageSource = messageSource;
    }

    @ModelAttribute("createEventForm")
    public CreateEventForm createEventForm() {
        return new CreateEventForm();
    }

    @GetMapping("/host/events/new")
    public ModelAndView showCreateEvent(final Locale locale) {
        final ModelAndView mav = new ModelAndView("host/create-event");
        mav.addObject("shell", ShellViewModelFactory.hostShell(messageSource, locale));
        mav.addObject("createEventForm", createEventForm());
        return mav;
    }

    @PostMapping("/host/events/new")
    public ModelAndView publishEvent(
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult,
            final Locale locale) {
        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null, locale);
        }

        final Instant startsAt =
                createEventForm
                        .getEventDate()
                        .atTime(createEventForm.getEventTime())
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

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
                        null,
                        createEventForm.getAddress(),
                        createEventForm.getTitle(),
                        createEventForm.getDescription(),
                        startsAt,
                        null,
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
            mav.addObject("shell", ShellViewModelFactory.hostShell(messageSource, locale));
            mav.addObject(
                    "title", messageSource.getMessage("verification.checkEmail", null, locale));
            mav.addObject(
                    "summary",
                    messageSource.getMessage(
                            "verification.publication.summary",
                            new Object[] {requestResult.getEmail()},
                            locale));
            mav.addObject(
                    "expiresAtLabel",
                    expiryFormatter(locale)
                            .format(requestResult.getExpiresAt().atZone(ZoneId.systemDefault())));
            mav.addObject("backHref", "/host/events/new");
            mav.addObject(
                    "actionLabel", messageSource.getMessage("host.backToCreate", null, locale));
            mav.addObject(
                    "eyebrow", messageSource.getMessage("host.publicationRequested", null, locale));
            return mav;
        } catch (final VerificationFailureException exception) {
            return hostFormView(createEventForm, exception.getMessage(), locale);
        }
    }

    private ModelAndView hostFormView(
            final CreateEventForm form, final String formError, final Locale locale) {
        final ModelAndView mav = new ModelAndView("host/create-event");
        mav.addObject("shell", ShellViewModelFactory.hostShell(messageSource, locale));
        mav.addObject("createEventForm", form);
        mav.addObject("formError", formError);
        return mav;
    }

    private static DateTimeFormatter expiryFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale == null ? Locale.ENGLISH : locale);
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
}
