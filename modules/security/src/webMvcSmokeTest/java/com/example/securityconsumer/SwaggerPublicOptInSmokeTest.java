package com.example.securityconsumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SwaggerPublicOptInSmokeTest.TestApplication.class,
        properties = {
                "jwt.secret=test-secret-key-that-is-at-least-32-characters-long",
                "jwt.access-token-ttl=1h",
                "jwt.refresh-token-ttl=7d",
                "security.swagger-public=true"
        }
)
@AutoConfigureMockMvc
class SwaggerPublicOptInSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger endpoint는 명시적으로 opt-in한 경우에만 공개한다")
    void Swagger_endpoint는_명시적으로_opt_in한_경우에만_공개한다() throws Exception {
        // given
        String swaggerEndpoint = "/v3/api-docs";

        // when & then
        mockMvc.perform(get(swaggerEndpoint))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("swagger"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(SwaggerController.class)
    static class TestApplication {
    }

    @RestController
    static class SwaggerController {

        @GetMapping("/v3/api-docs")
        Map<String, String> swagger() {
            return Map.of("status", "swagger");
        }
    }
}
