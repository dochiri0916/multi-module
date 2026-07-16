package com.dochiri.security.adapter.out.persistence.configuration;

import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.RefreshSessionTokenVerifierPort;
import com.dochiri.security.application.port.out.RefreshSessionRepositoryPort;
import com.dochiri.security.application.port.out.RotatingTokenIssuerPort;
import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.application.service.RotateTokensService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Import;

@AutoConfiguration(
        after = SecurityUseCaseAutoConfiguration.class,
        afterName = "com.dochiri.security.adapter.out.jwt.issuer.configuration.JwtIssuerAutoConfiguration"
)
@ConditionalOnBean({
        RotatingTokenIssuerPort.class,
        RefreshSessionTokenVerifierPort.class,
        TokenIdGeneratorPort.class,
        CurrentTimePort.class,
        RefreshSessionRepositoryPort.class
})
@Import(RotateTokensService.class)
public class SecurityRotationUseCaseAutoConfiguration {
}
