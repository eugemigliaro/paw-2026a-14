package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.utils.ErrorPageViews;
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
        return ErrorPageViews.build("404");
    }

    @GetMapping("/errors/400")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView showBadRequestPage() {
        return ErrorPageViews.build("400");
    }

    @GetMapping("/errors/405")
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ModelAndView showMethodNotAllowedPage() {
        return ErrorPageViews.build("405");
    }

    @GetMapping("/errors/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView showForbiddenPage() {
        return ErrorPageViews.build("403");
    }

    @GetMapping("/errors/409")
    @ResponseStatus(HttpStatus.CONFLICT)
    public ModelAndView showConflictPage() {
        return ErrorPageViews.build("409");
    }

    @GetMapping("/errors/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView showInternalServerErrorPage() {
        return ErrorPageViews.build("500");
    }
}
