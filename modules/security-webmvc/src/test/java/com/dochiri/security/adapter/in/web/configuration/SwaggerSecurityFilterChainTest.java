package com.dochiri.security.adapter.in.web.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SecurityFilterChainAutoConfigurationTest.SecurityWebApplication.class,
        properties = "security.swagger-public=true"
)
@AutoConfigureMockMvc
class SwaggerSecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger endpoint는 명시적으로 공개 설정하면 인증 없이 접근할 수 있다")
    void permitsSwaggerEndpointWhenExplicitlyConfigured() throws Exception {
        // given
        String endpoint = "/v3/api-docs";

        // when
        MvcResult result = mockMvc.perform(get(endpoint))
                .andExpect(status().isOk())
                .andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("\"status\":\"swagger\"");
    }
}
