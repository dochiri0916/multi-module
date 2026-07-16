package com.dochiri.security.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class TokenIdTest {

    @Test
    @DisplayName("토큰 식별자를 하이픈 없는 UUID 문자열로 생성한다")
    void generatesHyphenlessUuidTokenIdentifier() {
        // given
        int expectedLength = 32;

        // when
        TokenId tokenId = TokenId.generate();

        // then
        assertThat(tokenId.value())
                .hasSize(expectedLength)
                .matches("[0-9a-f]{32}");
    }

    @Test
    @DisplayName("Application 예외가 보관하는 토큰 식별자는 안전하게 직렬화할 수 있다")
    void serializesApplicationExceptionTokenIdentifierSafely()
            throws IOException, ClassNotFoundException {
        // given
        TokenId tokenId = new TokenId("token-id-01");

        // when
        byte[] serialized;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(tokenId);
            serialized = bytes.toByteArray();
        }
        TokenId restored;
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(serialized);
             ObjectInputStream input = new ObjectInputStream(bytes)) {
            restored = (TokenId) input.readObject();
        }

        // then
        assertThat(restored).isEqualTo(tokenId);
    }
}
