package com.dochiri.security.adapter.out.persistence;

import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.RevokedAt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshSessionBulkRevocationAdapterTest {

    @Test
    @DisplayName("전체 Refresh Session 폐기는 정규화된 subject와 시각을 JPA repository에 전달한다")
    void delegatesNormalizedSubjectAndRevocationTimeToJpaRepository() {
        // given
        RefreshSessionJpaRepository repository = mock(RefreshSessionJpaRepository.class);
        when(repository.revokeAllActiveBySubject("member-01", Instant.parse("2030-01-01T00:00:00Z")))
                .thenReturn(3);
        RefreshSessionBulkRevocationAdapter adapter = new RefreshSessionBulkRevocationAdapter(repository);

        // when
        int revokedCount = adapter.revokeAll(
                new AuthenticationSubject("  member-01  "),
                new RevokedAt(Instant.parse("2030-01-01T00:00:00Z"))
        );

        // then
        assertThat(revokedCount).isEqualTo(3);
        verify(repository).revokeAllActiveBySubject(
                "member-01",
                Instant.parse("2030-01-01T00:00:00Z")
        );
    }
}
