package com.example.securityconsumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = CustomSecurityFilterChainSmokeTest.CustomSecurityApplication.class,
        properties = {
                "jwt.secret=test-secret-key-that-is-at-least-32-characters-long",
                "jwt.access-token-ttl=1h",
                "jwt.refresh-token-ttl=7d"
        }
)
@AutoConfigureMockMvc
class CustomSecurityFilterChainSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    @DisplayName("사용자 SecurityFilterChain이 있으면 기본 chain이 물러난다")
    void customSecurityFilterChainOverridesDefault() throws Exception {
        // given
        String endpoint = "/custom-chain";

        // when & then
        assertThat(securityFilterChain).isNotNull();
        mockMvc.perform(get(endpoint)).andExpect(status().isOk());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(CustomSecurityController.class)
    static class CustomSecurityApplication {

        @Bean
        SecurityFilterChain customSecurityFilterChain() {
            return new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE);
        }
    }

    @RestController
    static class CustomSecurityController {

        @GetMapping("/custom-chain")
        Map<String, String> endpoint() {
            return Map.of("status", "ok");
        }
    }
}
