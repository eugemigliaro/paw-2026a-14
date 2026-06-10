package ar.edu.itba.paw.webapp.exception;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.exceptions.pagination.InvalidPaginationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

class GeneralExceptionHandlerTest {

    private MockMvc mockMvc;

    @Controller
    private static final class ThrowingController {

        @GetMapping("/throws-bad-request")
        public void throwsBadRequest() {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        @GetMapping("/throws-conflict")
        public void throwsConflict() {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        @GetMapping("/throws-generic")
        public void throwsGeneric() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/throws-invalid-pagination")
        public void throwsInvalidPagination() {
            throw new InvalidPaginationException("invalidPage");
        }

        @GetMapping("/method-only")
        public void methodOnly() {}

        @GetMapping("/typed-param")
        public void typedParam(@RequestParam("id") final Long id) {}
    }

    @BeforeEach
    void setUp() {
        final MessageSource messageSource = Mockito.mock(MessageSource.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ThrowingController())
                        .setControllerAdvice(new GeneralExceptionHandler(messageSource))
                        .build();
    }

    @Test
    void responseStatusBadRequestRenders400Page() throws Exception {
        mockMvc.perform(get("/throws-bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "400"));
    }

    @Test
    void responseStatusConflictRenders409Page() throws Exception {
        mockMvc.perform(get("/throws-conflict"))
                .andExpect(status().isConflict())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "409"));
    }

    @Test
    void methodMismatchRenders405PageAndAllowHeader() throws Exception {
        mockMvc.perform(post("/method-only"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "405"));
    }

    @Test
    void invalidTypedParameterRenders400Page() throws Exception {
        mockMvc.perform(get("/typed-param").param("id", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "400"));
    }

    @Test
    void invalidPaginationRenders400Page() throws Exception {
        mockMvc.perform(get("/throws-invalid-pagination"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "400"));
    }

    @Test
    void genericExceptionKeepsInternalServerErrorRedirect() throws Exception {
        mockMvc.perform(get("/throws-generic"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errors/500"));
    }
}
