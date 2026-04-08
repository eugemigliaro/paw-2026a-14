package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorPageController {

    @GetMapping("/errors/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView showNotFoundPage() {
        final ModelAndView mav = new ModelAndView("errors/not-found");
        mav.addObject("shell", ShellViewModelFactory.browseShell());
        return mav;
    }
}
