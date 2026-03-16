package ar.edu.itba.paw.webapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.services.UserService;

@Controller
public class HelloWorldController {

    private final UserService userService;

    @Autowired
    public HelloWorldController(final UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView helloWorld() {
        final ModelAndView mav = new ModelAndView("index.jsp");
        mav.addObject("message", "Hello World from HelloWorldController!");
        return mav;
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ModelAndView createUser(@RequestParam("email") final String email) {
        final ModelAndView mav = new ModelAndView("index.jsp");
        Object user = userService.createUser(email);
        mav.addObject("message", "Hello World " + user.toString());
        return mav;
    }

    @RequestMapping(value = "/button", method = RequestMethod.GET)
    public ModelAndView buttonShowcase() {
        return new ModelAndView("button.jsp");
    }
}
