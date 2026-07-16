package com.dochiri.security.adapter.in.web.authentication;

import com.dochiri.security.application.port.out.DecodedAccessToken;
import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import com.dochiri.security.domain.model.EncodedToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationConverter {

    private final AccessTokenVerifierPort accessTokenVerifierPort;

    public UsernamePasswordAuthenticationToken convert(EncodedToken token) {
        DecodedAccessToken decodedToken = accessTokenVerifierPort.verifyAccess(token);
        JwtPrincipal principal = new JwtPrincipal(decodedToken.subject(), decodedToken.role());
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(decodedToken.role().grantedAuthority()))
        );
    }
}
