package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.viewmodel.PawUiMockData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HostController {

    @GetMapping("/host/events/new")
    public ModelAndView showCreateEvent() {
        final ModelAndView mav = new ModelAndView("host/create-event");
        mav.addObject("shell", PawUiMockData.hostShell());
        mav.addObject("createPage", PawUiMockData.createEventPage());
        return mav;
    }
}
