package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.viewmodel.PawUiMockData;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventDetailPageViewModel;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class EventController {

    @GetMapping("/events/{eventId}")
    public ModelAndView showEventDetails(@PathVariable("eventId") final String eventId) {
        final Optional<EventDetailPageViewModel> eventPage = PawUiMockData.findEventPage(eventId);

        if (eventPage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        final ModelAndView mav = new ModelAndView("events/detail");
        mav.addObject("shell", PawUiMockData.browseShell());
        mav.addObject("eventPage", eventPage.get());
        return mav;
    }
}
