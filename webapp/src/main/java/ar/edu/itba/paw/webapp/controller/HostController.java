package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.ActionVerificationService;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.webapp.form.CreateEventForm;
import ar.edu.itba.paw.webapp.viewmodel.PawUiMockData;
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

    @Autowired
    public HostController(final ActionVerificationService actionVerificationService) {
        this.actionVerificationService = actionVerificationService;
    }

    @ModelAttribute("createEventForm")
    public CreateEventForm createEventForm() {
        return new CreateEventForm();
    }

    @GetMapping("/host/events/new")
    public ModelAndView showCreateEvent() {
        final ModelAndView mav = new ModelAndView("host/create-event");
        mav.addObject("shell", PawUiMockData.hostShell());
        mav.addObject("createPage", PawUiMockData.createEventPage());
        mav.addObject("createEventForm", createEventForm());
        return mav;
    }

    @PostMapping("/host/events/new")
    public ModelAndView publishEvent(
            @Valid @ModelAttribute("createEventForm") final CreateEventForm createEventForm,
            final BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return hostFormView(createEventForm, null);
        }

        final Instant startsAt =
                createEventForm
                        .getEventDate()
                        .atTime(createEventForm.getEventTime())
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

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
                        "open");

        try {
            final VerificationRequestResult requestResult =
                    actionVerificationService.requestMatchCreation(
                            request, createEventForm.getEmail());
            final ModelAndView mav = new ModelAndView("verification/check-email");
            mav.addObject("shell", PawUiMockData.hostShell());
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
        mav.addObject("shell", PawUiMockData.hostShell());
        mav.addObject("createPage", PawUiMockData.createEventPage());
        mav.addObject("createEventForm", form);
        mav.addObject("formError", formError);
        return mav;
    }
}
