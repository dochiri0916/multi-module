# Time Module

`modules:time`은 모놀리식 애플리케이션과 MSA 서비스에서 같은 시간 기준을 사용하도록 `Clock` bean과 timezone 설정을 제공하는 모듈이다.

## 현재 구현

- `TimeProperties`
  - prefix: `time`
  - property: `timezone`
  - 값이 없으면 `Asia/Seoul` 사용
- `TimeAutoConfiguration`
  - `TimeProperties`를 활성화한다.
  - `Clock.system(ZoneId.of(properties.timezone()))` bean을 등록한다.
- auto-configuration imports
  - `com.dochiri.time.configuration.TimeAutoConfiguration`

## 문제점

### 1. 소비 프로젝트의 `Clock` 커스터마이징을 막을 수 있음

현재 `Clock` bean은 항상 등록된다. 소비 프로젝트가 테스트, 멀티 타임존, 배치 작업 등에서 자체 `Clock`을 등록하고 싶을 때 충돌할 수 있다.

### 2. timezone 검증이 약함

`timezone` 값이 blank이면 기본값을 쓰지만, 잘못된 zone id면 `Clock` bean 생성 시점에 `DateTimeException`이 발생한다. 설정 오류 메시지가 모듈 계약 관점에서 명확하지 않다.

### 3. 기본 timezone 정책이 코드에만 있음

기본값 `Asia/Seoul`이 코드에 박혀 있다. 문서와 configuration metadata에서 소비자가 바로 확인할 수 있게 정리해야 한다.

### 4. 테스트용 고정 시간 지원이 없음

운영 코드에서는 `Clock` 주입만으로 충분하지만, 소비 프로젝트에서 테스트 편의를 위해 고정 `Clock`을 어떻게 등록해야 하는지 사용 예시가 필요하다.

## 리팩토링 방향

### 1단계: `Clock` bean 조건부 등록

`TimeAutoConfiguration`의 `Clock` bean에 `@ConditionalOnMissingBean(Clock.class)`를 추가한다.

목표:

- 소비 프로젝트가 자체 `Clock` bean을 등록하면 모듈 기본 bean은 물러난다.
- 테스트와 특수 서비스에서 시간 기준을 쉽게 대체할 수 있다.

### 2단계: timezone 설정 검증 강화

`TimeProperties`에서 timezone을 normalize하고, 잘못된 zone id는 명확한 예외 메시지로 실패하게 한다.

검토안:

```java
@ConfigurationProperties(prefix = "time")
public record TimeProperties(String timezone) {

    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }
}
```

또는 생성자에서 `ZoneId.of(...)`로 검증하고 canonical id를 보관한다.

### 3단계: configuration metadata 보강

`spring-boot-configuration-processor`가 생성하는 metadata에 설명이 잘 드러나도록 property 문서와 README 사용 예시를 정리한다.

문서화할 항목:

- `time.timezone`
- 기본값: `Asia/Seoul`
- 예시: `UTC`, `Asia/Seoul`

### 4단계: 테스트 보강

테스트 대상:

- 기본 timezone이 `Asia/Seoul`인지
- `time.timezone=UTC` 설정 시 `Clock` zone이 UTC인지
- 소비 프로젝트가 직접 등록한 `Clock` bean이 우선되는지
- 잘못된 timezone 설정 시 실패하는지

## 사용 예시

```yaml
time:
  timezone: Asia/Seoul
```

```java
@Service
class PostService {

    private final Clock clock;

    PostService(Clock clock) {
        this.clock = clock;
    }

    Instant now() {
        return Instant.now(clock);
    }
}
```

테스트에서 고정 시간이 필요하면 소비 프로젝트가 직접 `Clock` bean을 등록한다.

```java
@TestConfiguration
class FixedClockTestConfiguration {

    @Bean
    Clock clock() {
        return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
}
```

## 완료 기준

- 소비 프로젝트가 직접 등록한 `Clock`이 우선된다.
- 잘못된 timezone 설정은 명확하게 실패한다.
- 기본값과 설정 방법이 문서화되어 있다.
- `./gradlew :modules:time:test`가 통과한다.
