package de.htw.studyplanner.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import de.htw.studyplanner.backend.TestcontainersConfiguration;

/**
 * Prüft, dass die OpenAPI-Spezifikation erreichbar ist und die dokumentierten
 * Statuscodes enthält. Das Frontend baut gegen genau diese Spezifikation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void spezifikationIstUnterApiDocsAbrufbar() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Studyplanner API"))
                .andExpect(jsonPath("$.paths['/api/health'].get").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/modules'].get").exists())
                .andExpect(jsonPath("$.paths['/api/modules/{code}'].get").exists());
    }

    @Test
    void endpunkteHabenIhreStatuscodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/modules'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/modules'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/modules/{code}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/modules/{code}'].get.responses['404']").exists());
    }

    @Test
    void healthHatKeineFremdenFehlercodes() throws Exception {
        // springdoc.override-with-generic-response=false: die Fehler aus dem
        // ApiExceptionHandler dürfen nicht an jedem Endpunkt auftauchen.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/health'].get.responses['409']").doesNotExist());
    }

    @Test
    void swaggerUiIstErreichbar() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
