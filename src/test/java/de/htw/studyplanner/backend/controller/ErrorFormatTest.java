package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.security.ApiErrorWriter;
import de.htw.studyplanner.backend.security.RestAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ErrorFormatTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void kaputtesJsonGibt400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{kaputt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void validierungGibtAlleFeldfehlerZurueck() throws Exception {
        // Pfad und Felder an deinen Registrierungs-Endpunkt anpassen
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()", greaterThan(1)))
                .andExpect(jsonPath("$.errors[0].field").exists())
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    void ohneTokenGibt401() throws Exception {
        mockMvc.perform(get("/api/modules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Bitte melde dich an, um fortzufahren."));
    }

    @Test
    @WithMockUser
    void forbiddenGibt403() throws Exception {
        mockMvc.perform(get("/api/test-errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser
    void notFoundGibt404() throws Exception {
        mockMvc.perform(get("/api/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Modul mit der ID 42 wurde nicht gefunden."))
                .andExpect(jsonPath("$.path").value("/api/test-errors/not-found"));
    }

    @Test
    @WithMockUser
    void conflictGibt409() throws Exception {
        mockMvc.perform(get("/api/test-errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Diese E-Mail-Adresse ist bereits vergeben."));
    }

    @Test
    @WithMockUser
    void unerwarteterFehlerGibt500OhneInterna() throws Exception {
        mockMvc.perform(get("/api/test-errors/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("com.example"))));
    }

    @Test
    void accessDeniedHandlerSchreibtEinheitlichesFormat() throws Exception {
        var handler = new RestAccessDeniedHandler(new ApiErrorWriter(new ObjectMapper()));
        var request = new MockHttpServletRequest("GET", "/api/modules");
        var response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("x"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("\"status\":403")
                .contains("\"path\":\"/api/modules\"");
    }
}