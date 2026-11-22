package de.htw.studyplanner.backend.controller;

import static org.hamcrest.Matchers.containsString;
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
                .andExpect(jsonPath("$.paths['/api/auth/register/professor'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register/professor'].post.requestBody"
                        + ".content['application/json'].examples").exists())
                .andExpect(jsonPath("$.paths['/api/modules'].get").exists())
                .andExpect(jsonPath("$.paths['/api/modules/{code}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/offers'].get").exists())
                .andExpect(jsonPath("$.components.schemas.ModuleDetailDto.properties.offers").exists())
                .andExpect(jsonPath("$.components.schemas.OfferDto.properties.recommendedModuleCode").exists())
                .andExpect(jsonPath("$.components.schemas.PlanResponse.properties.semesters").exists())
                .andExpect(jsonPath("$.components.schemas.PlanSemesterDto.properties.entries").exists())
                .andExpect(jsonPath("$.components.schemas.PlanEntryDto.properties.prerequisitesMet").exists())
                .andExpect(jsonPath("$.components.schemas.PlanEntryDto.properties.violations").exists())
                .andExpect(jsonPath("$.paths['/api/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/plan'].get").exists())
                .andExpect(jsonPath("$.paths['/api/plan/entries'].post").exists())
                .andExpect(jsonPath("$.paths['/api/statistics/cohorts'].get").exists())
                .andExpect(jsonPath("$.components.schemas.CohortStatisticsDto").exists())
                .andExpect(jsonPath("$.paths['/api/progress'].get").exists())
                .andExpect(jsonPath("$.components.schemas.ProgressResponse.properties.openModules").exists())
                .andExpect(jsonPath("$.components.schemas.OpenModuleDto.properties.lockReasons").exists());
    }
// Prüft, dass die dokumentierten Statuscodes in der OpenAPI-Spezifikation enthalten sind
    @Test
    void endpunkteHabenIhreStatuscodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register/professor'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register/professor'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register/professor'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/modules'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/modules'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/modules/{code}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/modules/{code}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/me'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/me'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/me'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/results'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/results/{moduleCode}'].put.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/results/{moduleCode}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/plan/entries'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/plan/entries'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/plan/entries'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/plan/entries'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/plan/entries'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/plan/entries'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/plan/entries'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/plan'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/plan'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/plan'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/plan'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/statistics/cohorts'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/statistics/cohorts'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/statistics/cohorts'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/statistics/cohorts'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/progress'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/progress'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/progress'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/progress'].get.security[0].bearerAuth").exists());
    }
// Prüft, dass die Health-Endpoint keine fremden Fehlercodes enthält (z. B. 409 Conflict)
    @Test
    void healthHatKeineFremdenFehlercodes() throws Exception {
        // springdoc.override-with-generic-response=false: die Fehler aus dem
        // ApiExceptionHandler dürfen nicht an jedem Endpunkt auftauchen.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/health'].get.responses['409']").doesNotExist());
    }
// Prüft, dass die Registrierung und der Login die erlaubten HTW-Domains in der Beschreibung nennen
    @Test
    void registrierungUndLoginBeschreibenDieHtwEmailRegel() throws Exception {
        // US-28: Statuscodes, Beschreibung und Beispielwerte nennen die erlaubten HTW-Domains.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.description")
                        .value(containsString("@student.htw-berlin.de")))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.description")
                        .value(containsString("@student.htw-berlin.de")))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.description")
                        .value(containsString("`@htw-berlin.de`")))
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.email.example")
                        .value("studentin@student.htw-berlin.de"))
                .andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.email.example")
                        .value("studentin@student.htw-berlin.de"));
    }
// Prüft, dass die Swagger-UI erreichbar ist
    @Test
    void swaggerUiIstErreichbar() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
