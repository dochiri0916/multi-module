# Time Module

`modules:time`은 소비 애플리케이션에 일관된 `Clock` Bean을 제공하는 작은 기술 모듈이다. 비즈니스 시간 정책은 소비 Context가 소유한다.

## 설정

```yaml
time:
  timezone: Asia/Seoul
```

`TimeProperties`는 `@ConfigurationProperties(prefix = "time")` record다. `timezone`이 null 또는 blank이면 `Asia/Seoul`을 사용한다.

지원 예시:

- `Asia/Seoul`
- `UTC`
- 표준 `ZoneId` 문자열

잘못된 Zone ID는 `Clock` Bean 구성 시 즉시 실패한다.

## 자동 구성

```text
com.dochiri.time.configuration.TimeAutoConfiguration
```

기본 Bean:

```java
Clock.system(ZoneId.of(properties.timezone()))
```

`@ConditionalOnMissingBean(Clock.class)`를 적용하므로 소비자가 `Clock`을 제공하면 기본 Bean은 물러난다.

## 사용

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-time:0.0.1-SNAPSHOT'
}
```

시간에 의존하는 코드에는 `Clock`을 생성하지 않고 주입한다.

```java
@Service
@RequiredArgsConstructor
public class ExpirationService {

    private final Clock clock;

    public Instant currentTime() {
        return Instant.now(clock);
    }
}
```

결과에 영향을 주는 시간은 테스트에서 고정한다.

```java
@TestConfiguration
class FixedClockConfiguration {

    @Bean
    Clock fixedClock() {
        return Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );
    }
}
```

## 경계

- Domain이 Spring `Clock`을 직접 참조하지 않는다.
- 재현 가능한 Application 흐름은 `CurrentTimePort` 같은 소비자 소유 Port로 번역한다.
- `security-jwt`는 주입된 JDK `Clock`으로 JWT 검증을 결정적으로 수행한다.
- timezone에 따른 영업일, 마감, 휴일 규칙은 이 모듈에 넣지 않는다.

## 검증

```bash
./gradlew :modules:time:test
./gradlew check
```

테스트는 기본/사용자 timezone, blank 기본값, 잘못된 Zone ID, 소비자 `Clock` back-off를 확인한다.
