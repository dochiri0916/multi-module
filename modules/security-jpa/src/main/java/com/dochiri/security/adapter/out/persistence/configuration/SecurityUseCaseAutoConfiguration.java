package com.dochiri.security.adapter.out.persistence.configuration;

import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.RefreshSessionBulkRevocationPort;
import com.dochiri.security.application.port.out.RefreshSessionPort;
import com.dochiri.security.application.port.out.RefreshTokenVerifierPort;
import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.application.port.out.TokenIssuerPort;
import com.dochiri.security.application.service.IssueTokensService;
import com.dochiri.security.application.service.RevokeAllRefreshTokensService;
import com.dochiri.security.application.service.RevokeRefreshTokenService;
import com.dochiri.security.application.service.VerifyRefreshTokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@AutoConfiguration(
        after = SecurityJpaAutoConfiguration.class,
        afterName = "com.dochiri.security.adapter.out.jwt.issuer.configuration.JwtIssuerAutoConfiguration"
)
@ConditionalOnBean({
        TokenIssuerPort.class,
        RefreshTokenVerifierPort.class,
        TokenIdGeneratorPort.class,
        CurrentTimePort.class,
        RefreshSessionPort.class,
        RefreshSessionBulkRevocationPort.class
})
@Import({
        IssueTokensService.class,
        VerifyRefreshTokenService.class,
        RevokeRefreshTokenService.class,
        RevokeAllRefreshTokensService.class
})
@Component
public class SecurityUseCaseAutoConfiguration {
}
