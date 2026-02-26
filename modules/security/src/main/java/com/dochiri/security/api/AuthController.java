package com.dochiri.security.api;

import com.dochiri.security.jwt.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Long DEMO_USER_ID = 1L;
    private static final String DEMO_PASSWORD = "password123!";

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final String demoPasswordHash;

    public AuthController(JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.demoPasswordHash = passwordEncoder.encode(DEMO_PASSWORD);
    }

    @PostMapping("/token")
    public TokenResponse issueToken(@RequestBody TokenIssueRequest request) {
        if (request == null || request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        String token = jwtTokenProvider.generateAccessToken(String.valueOf(request.userId()));
        return new TokenResponse(token);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        if (request == null || request.userId() == null || !StringUtils.hasText(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and password are required");
        }

        boolean isValidUser = DEMO_USER_ID.equals(request.userId());
        boolean isValidPassword = passwordEncoder.matches(request.password(), demoPasswordHash);
        if (!isValidUser || !isValidPassword) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }

        String token = jwtTokenProvider.generateAccessToken(String.valueOf(request.userId()));
        return new TokenResponse(token);
    }

    public record LoginRequest(Long userId, String password) {
    }

    public record TokenIssueRequest(Long userId) {
    }

    public record TokenResponse(String accessToken) {
    }
}
