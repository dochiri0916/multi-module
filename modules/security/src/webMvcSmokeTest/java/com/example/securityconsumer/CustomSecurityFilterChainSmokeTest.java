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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = CustomSecurityFilterChainSmokeTest.TestApplication.class,
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
    void 사용자_SecurityFilterChain이_있으면_기본_chain이_물러난다() throws Exception {
        // given
        String endpoint = "/custom-chain";

        // when & then
        assertThat(securityFilterChain).isNotNull();
        mockMvc.perform(get(endpoint)).andExpect(status().isOk());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(CustomController.class)
    static class TestApplication {

        @Bean
        SecurityFilterChain customSecurityFilterChain(HttpSecurity http) {
            try {
                return http
                        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                        .build();
            } catch (Exception exception) {
                throw new IllegalStateException("소비자 SecurityFilterChain을 구성할 수 없습니다.", exception);
            }
        }
    }

    @RestController
    static class CustomController {

        @GetMapping("/custom-chain")
        Map<String, String> endpoint() {
            return Map.of("status", "ok");
        }
    }
}
