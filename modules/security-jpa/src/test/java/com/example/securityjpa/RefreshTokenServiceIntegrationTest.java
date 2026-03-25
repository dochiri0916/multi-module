package com.example.securityjpa;

import com.dochiri.security.jpa.repository.RefreshTokenRepository;
import com.dochiri.security.jpa.service.RefreshTokenService;
import com.dochiri.security.jwt.JwtTokenResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = RefreshTokenServiceIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:securityjpa;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "time.timezone=Asia/Seoul",
                "jwt.secret=test-secret-key-that-is-at-least-32-characters-long",
                "jwt.access-expiration=3600000",
                "jwt.refresh-expiration=604800000",
                "security.system-user-id=0"
        }
)
class RefreshTokenServiceIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void 공통_모듈만으로_RefreshTokenRepository와_Service를_사용할_수_있다() {
        JwtTokenResult tokenResult = refreshTokenService.generateToken(1L, "USER");

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        assertThat(refreshTokenService.verifyAndExtractUserId(tokenResult.refreshToken())).isEqualTo(1L);
    }

    @Test
    void 폐기된_리프레시_토큰은_다시_검증할_수_없다() {
        JwtTokenResult tokenResult = refreshTokenService.generateToken(1L, "USER");

        boolean revoked = refreshTokenService.revoke(tokenResult.refreshToken());

        assertThat(revoked).isTrue();
        assertThatThrownBy(() -> refreshTokenService.verifyAndExtractUserId(tokenResult.refreshToken()))
                .hasMessageContaining("사용할 수 없는");
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
