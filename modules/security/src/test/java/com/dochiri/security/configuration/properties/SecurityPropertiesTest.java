package com.dochiri.security.configuration.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPropertiesTest {

    @Test
    void publicEndpoints가_null이면_빈_리스트가_된다() {
        SecurityProperties properties = new SecurityProperties(null, null);

        assertThat(properties.publicEndpoints()).isEmpty();
    }

    @Test
    void systemUserId가_null이면_기본값_0이_된다() {
        SecurityProperties properties = new SecurityProperties(null, null);

        assertThat(properties.systemUserId()).isEqualTo(0L);
    }

    @Test
    void 값을_명시하면_해당_값이_사용된다() {
        SecurityProperties properties = new SecurityProperties(
                List.of("/api/auth/**"), 999L);

        assertThat(properties.publicEndpoints()).containsExactly("/api/auth/**");
        assertThat(properties.systemUserId()).isEqualTo(999L);
    }
}
