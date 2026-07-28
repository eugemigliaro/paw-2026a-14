package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ErrorPageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorPageController()).build();
    }

    @Test
    void getNotFoundErrorRouteRenders404Page() throws Exception {
        mockMvc.perform(get("/errors/404"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "404"));
    }

    @Test
    void getBadRequestErrorRouteRenders400Page() throws Exception {
        mockMvc.perform(get("/errors/400"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "400"));
    }

    @Test
    void getMethodNotAllowedErrorRouteRenders405Page() throws Exception {
        mockMvc.perform(get("/errors/405"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "405"));
    }

    @Test
    void getForbiddenErrorRouteRenders403Page() throws Exception {
        mockMvc.perform(get("/errors/403"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "403"));
    }

    @Test
    void getConflictErrorRouteRenders409Page() throws Exception {
        mockMvc.perform(get("/errors/409"))
                .andExpect(status().isConflict())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "409"));
    }

    @Test
    void getInternalServerErrorRouteRenders500Page() throws Exception {
        mockMvc.perform(get("/errors/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "500"));
    }

    @Test
    void postNotFoundErrorRouteRenders404Page() throws Exception {
        mockMvc.perform(post("/errors/404"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "404"));
    }

    @Test
    void postBadRequestErrorRouteRenders400Page() throws Exception {
        mockMvc.perform(post("/errors/400"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "400"));
    }

    @Test
    void postMethodNotAllowedErrorRouteRenders405Page() throws Exception {
        mockMvc.perform(post("/errors/405"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "405"));
    }

    @Test
    void postForbiddenErrorRouteRenders403Page() throws Exception {
        mockMvc.perform(post("/errors/403"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "403"));
    }

    @Test
    void postConflictErrorRouteRenders409Page() throws Exception {
        mockMvc.perform(post("/errors/409"))
                .andExpect(status().isConflict())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "409"));
    }

    @Test
    void postInternalServerErrorRouteRenders500Page() throws Exception {
        mockMvc.perform(post("/errors/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attribute("number", "500"));
    }
}
