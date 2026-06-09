package ar.edu.itba.paw.webapp.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.exceptions.ForbiddenException;
import ar.edu.itba.paw.models.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

class AccessExceptionHandlerTest {

    private MockMvc mockMvc;

    @Controller
    private static final class ThrowingController {

        @GetMapping("/throws-not-found")
        public void throwsNotFound() {
            throw new NotFoundException("missing");
        }

        @GetMapping("/throws-forbidden")
        public void throwsForbidden() {
            throw new ForbiddenException("denied");
        }
    }

    @BeforeEach
    void setUp() {
        final MessageSource messageSource = Mockito.mock(MessageSource.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ThrowingController())
                        .setControllerAdvice(new AccessExceptionHandler(messageSource))
                        .build();
    }

    @Test
    void notFoundExceptionRendersErrorPageWith404Status() throws Exception {
        mockMvc.perform(get("/throws-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "404"));
    }

    @Test
    void forbiddenExceptionRendersErrorPageWith403Status() throws Exception {
        mockMvc.perform(get("/throws-forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "403"));
    }
}
