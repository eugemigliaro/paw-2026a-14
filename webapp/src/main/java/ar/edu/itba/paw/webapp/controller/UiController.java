package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.viewmodel.PawUiMockData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UiController {

    @GetMapping("/ui/components")
    public ModelAndView showComponents() {
        final ModelAndView mav = new ModelAndView("ui/components");
        mav.addObject("shell", PawUiMockData.browseShell());
        mav.addObject("componentPage", PawUiMockData.componentPreviewPage());
        return mav;
    }
}
