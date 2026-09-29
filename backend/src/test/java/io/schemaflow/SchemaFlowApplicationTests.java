package io.schemaflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=demo")
@AutoConfigureMockMvc
class SchemaFlowApplicationTests {
    @Autowired
    private MockMvc mvc;

    @Test
    void healthAndSecurityHeadersArePresent() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void dashboardReportsDeterministicMigrationMetrics() throws Exception {
        mvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("deterministic-demo"))
                .andExpect(jsonPath("$.documentCount", greaterThan(10)))
                .andExpect(jsonPath("$.sourceRowCount", greaterThan(20)))
                .andExpect(jsonPath("$.collections", hasSize(2)));
    }

    @Test
    void documentsAreSearchableButInputsAreBounded() throws Exception {
        mvc.perform(get("/api/v1/documents").param("collection", "customer_profiles").param("query", "Tunis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].collection").value("customer_profiles"));

        mvc.perform(get("/api/v1/documents").param("limit", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void demoMigrationIsExplicitAndAuditable() throws Exception {
        mvc.perform(post("/api/v1/migrations/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.validationErrors").value(0));
    }

    @Test
    void architectureDisclosesPipelineAndSafetyControls() throws Exception {
        mvc.perform(get("/api/v1/architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages", hasSize(5)))
                .andExpect(jsonPath("$.safetyControls", hasSize(4)));
    }
}
