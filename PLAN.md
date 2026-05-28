# Multi-Module Library Restructure Plan

## 목표

이 레포는 모놀리식 애플리케이션이나 MSA 서비스를 직접 구현하지 않는다. 대신 공통 기능을 멀티 모듈 라이브러리 형태로 만들고, 빌드 산출물을 Docker Hub 기반 배포 경로를 통해 외부 프로젝트에서 의존받아 사용하게 한다.

소비 프로젝트는 다음 두 형태를 모두 지원해야 한다.

- 모놀리식 프로젝트: 하나의 Spring Boot 앱에서 필요한 공통 모듈을 한 번에 의존
- MSA 프로젝트: 각 서비스가 필요한 공통 모듈만 선택적으로 의존

핵심 목표는 다음과 같다.

- 이 레포에는 비즈니스 `domain`, 실행 `apps`, 특정 서비스 구현을 두지 않는다.
- 공통 기능은 작고 명확한 라이브러리 모듈로 분리한다.
- 빠른 시작이 필요한 프로젝트를 위해 starter 모듈을 제공한다.
- 모놀리식과 MSA 모두 같은 artifact를 의존받아 사용할 수 있게 한다.
- Docker Hub에는 소비 프로젝트가 참조할 수 있는 배포용 이미지 또는 artifact publishing 이미지를 올린다.
- 각 모듈은 Maven/Gradle 의존성으로 사용할 수 있게 versioning, publishing, 문서화를 정리한다.

## 기본 방향

현재 구조는 공통 라이브러리 레포라는 방향이 맞다.

```text
modules
├── api-starter
├── error-handling
├── jpa
├── security
├── security-jpa
└── time
```

다만 앞으로는 모놀리식과 MSA 모두에서 조합 가능하도록 모듈 책임과 의존 방향을 더 명확히 한다.

## 목표 모듈 구조

도메인/앱 모듈은 만들지 않고, 공통 라이브러리 계층만 둔다.

```text
.
├── modules
│   ├── core
│   ├── error-handling
│   ├── time
│   ├── web
│   ├── jpa
│   ├── security
│   ├── security-jpa
│   └── api-starter
├── publishing
│   └── docker
└── docs
    ├── monolith-usage.md
    └── msa-usage.md
```

초기에는 현재 모듈을 유지하면서 필요한 모듈만 추가한다. 큰 rename은 artifact 호환성 문제가 있으므로 마지막 단계에서 결정한다.

## 모듈 역할

### `modules:core`

모든 공통 모듈이 공유할 수 있는 가장 작은 기반 모듈이다.

- 공통 marker interface
- 공통 utility 중 기술 의존성이 없는 것
- 공통 contract type

주의:

- Spring Boot, Web, JPA, Security 의존성을 넣지 않는다.
- 비즈니스 도메인 타입은 넣지 않는다.
- 단순히 “여기저기 쓰인다”는 이유로 무거운 공통 모듈로 만들지 않는다.

### `modules:error-handling`

예외 계약과 API 에러 응답 기반을 제공한다.

- `BaseException`
- `ErrorCode`
- 공통 에러 응답 모델
- Web MVC 기반 `GlobalExceptionHandler`

개편 포인트:

- 도메인/application 계층에서도 쓸 수 있는 예외 계약과 Web MVC handler 책임을 분리할지 검토한다.
- 분리한다면 `error-handling-core`, `error-handling-web`처럼 나누거나 `web` 모듈로 handler를 이동한다.

### `modules:time`

시간 정책과 `Clock` 설정을 제공한다.

- timezone properties
- `Clock` bean auto configuration
- 테스트 가능한 시간 처리 기반

주의:

- 소비 프로젝트의 비즈니스 시간 정책을 대신 구현하지 않는다.
- 기본값과 override 방법을 문서화한다.

### `modules:web`

공통 Web MVC 설정을 모으는 모듈이다.

- validation 관련 기본 설정
- swagger/openapi 기본 설정
- 공통 JSON/ObjectMapper 설정이 필요하면 이곳에 배치
- 공통 web exception handling 연결

현재 `api-starter`에 들어간 web 관련 의존과 설정을 필요하면 이 모듈로 분리한다.

### `modules:jpa`

JPA 공통 기반을 제공한다.

- `BaseEntity`
- JPA auditing
- QueryDSL `JPAQueryFactory`
- JPA 관련 auto configuration

주의:

- 소비 프로젝트의 User, Member, Order 같은 비즈니스 엔티티를 제공하지 않는다.
- DB driver는 소비 프로젝트가 직접 선택한다.

### `modules:security`

DB에 의존하지 않는 보안 기반을 제공한다.

- JWT 발급/검증
- access token filter
- security filter chain 기본값
- CORS 설정
- 인증 principal
- auditing 연동을 위한 current user provider

주의:

- 사용자 도메인, 회원가입, 로그인 유스케이스를 구현하지 않는다.
- 인증 방식이 다른 프로젝트에서도 일부만 가져갈 수 있도록 JPA 의존을 두지 않는다.

### `modules:security-jpa`

JPA가 필요한 보안 persistence 기능을 제공한다.

- refresh token entity
- refresh token repository
- refresh token service

주의:

- `security`와 `jpa`를 조합한 선택 모듈로 유지한다.
- refresh token 저장소가 Redis 등으로 바뀔 수 있으므로 추후 `security-redis` 같은 별도 모듈 확장을 고려한다.

### `modules:api-starter`

가장 일반적인 API 서버 구성을 한 번에 제공하는 starter다.

포함 대상:

- Web MVC
- validation
- swagger/openapi
- error-handling
- time
- jpa
- security
- security-jpa

사용 대상:

- 빠르게 시작하는 모놀리식 API
- 각 MSA 서비스가 동일한 기본 API 구성을 사용하고 싶을 때

주의:

- 모든 프로젝트에 강제할 수 없는 기능은 starter에 넣기 전에 개별 모듈로 먼저 제공한다.
- starter는 편의 모듈이지 유일한 사용 경로가 아니다.

## 의존성 규칙

허용 방향:

```text
api-starter -> web
api-starter -> error-handling
api-starter -> time
api-starter -> jpa
api-starter -> security-jpa

security-jpa -> security
security-jpa -> jpa
security-jpa -> time

web -> error-handling
web -> time

jpa -> time

error-handling -> core
time -> core
web -> core
jpa -> core
security -> core
```

금지 방향:

```text
core -> Spring Boot / Web / JPA / Security
error-handling -> jpa
error-handling -> security
time -> web / jpa / security
jpa -> web / security
security -> jpa
security -> security-jpa
```

이 규칙의 목적은 소비 프로젝트가 필요한 기능만 선택해서 의존할 수 있게 하는 것이다.

## 모놀리식 프로젝트 사용 방식

모놀리식 프로젝트는 보통 `api-starter` 하나로 시작한다.

```gradle
repositories {
    mavenCentral()
    // 배포 방식 확정 후 Docker Hub 기반 artifact 접근 경로 또는 사내 registry 경로를 문서화한다.
}

dependencies {
    implementation 'com.dochiri:dochiri-api-starter:<version>'
    runtimeOnly 'com.mysql:mysql-connector-j'
}
```

필요한 경우 starter 대신 개별 모듈을 조합한다.

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-web:<version>'
    implementation 'com.dochiri:dochiri-error-handling:<version>'
    implementation 'com.dochiri:dochiri-time:<version>'
    implementation 'com.dochiri:dochiri-jpa:<version>'
    implementation 'com.dochiri:dochiri-security:<version>'
}
```

## MSA 프로젝트 사용 방식

MSA에서는 서비스별로 필요한 모듈만 선택한다.

예시: API + JPA + JWT가 필요한 서비스

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-api-starter:<version>'
    runtimeOnly 'org.postgresql:postgresql'
}
```

예시: DB 없이 JWT 검증만 필요한 gateway/service

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-web:<version>'
    implementation 'com.dochiri:dochiri-security:<version>'
}
```

예시: batch 또는 worker처럼 Web이 필요 없는 서비스

```gradle
dependencies {
    implementation 'com.dochiri:dochiri-time:<version>'
    implementation 'com.dochiri:dochiri-jpa:<version>'
}
```

## Docker Hub 배포 전략

Docker Hub는 Java library dependency를 직접 제공하는 Maven repository가 아니다. 따라서 배포 전략을 명확히 선택해야 한다.

선택지는 다음과 같다.

1. Docker image 안에 Maven repository 형태로 artifact를 포함한다.
   - 이미지 예: `dochiri/multi-module-repo:<version>`
   - 소비 프로젝트는 CI에서 이미지를 받아 artifact를 로컬/사내 Maven repo로 publish하거나 mount해서 사용한다.
   - 단점: 일반적인 Gradle dependency resolution과 바로 연결되지 않는다.

2. Docker image를 artifact publisher로 사용한다.
   - 이미지 예: `dochiri/multi-module-publisher:<version>`
   - 이미지 실행 시 GitHub Packages, Nexus, Artifactory 같은 Maven registry로 publish한다.
   - 소비 프로젝트는 Maven registry에서 일반 의존성으로 받는다.
   - 권장: Gradle/Maven 생태계와 가장 잘 맞는다.

3. Docker image는 샘플/검증용으로만 제공하고, 실제 artifact는 Maven registry에 publish한다.
   - 이미지 예: `dochiri/multi-module-sample:<version>`
   - library artifact는 별도 Maven repository에 배포한다.
   - 권장: 운영 사용성이 가장 단순하다.

권장 방향:

- 실제 의존성 배포는 Maven repository를 사용한다.
- Docker Hub에는 publishing image 또는 artifact bundle image를 올린다.
- 소비 프로젝트 문서에는 “Docker Hub 이미지 사용 방식”과 “Gradle dependency 선언 방식”을 분리해서 설명한다.

## Artifact 및 이미지 이름 규칙

Maven artifact:

```text
com.dochiri:dochiri-core
com.dochiri:dochiri-error-handling
com.dochiri:dochiri-time
com.dochiri:dochiri-web
com.dochiri:dochiri-jpa
com.dochiri:dochiri-security
com.dochiri:dochiri-security-jpa
com.dochiri:dochiri-api-starter
```

Docker image:

```text
dochiri/multi-module-publisher:<version>
dochiri/multi-module-artifacts:<version>
```

Version tag:

```text
0.1.0
0.1.1
0.2.0
latest
```

`latest`는 개발 편의용으로만 사용하고, 소비 프로젝트 문서에서는 고정 version 사용을 권장한다.

## Gradle 개편 계획

1. 루트 `build.gradle`의 공통 설정은 유지한다.
2. 모든 라이브러리 모듈은 `java-library`, `maven-publish`를 사용한다.
3. Spring Boot 실행 플러그인은 적용하지 않는다.
4. `api`와 `implementation` 의존성 경계를 재검토한다.
   - 소비 프로젝트에 노출되어야 하는 타입은 `api`
   - 내부 구현에만 필요한 의존성은 `implementation`
5. publishing metadata를 정리한다.
   - artifactId
   - description
   - version
   - pom dependency
6. Docker image build task를 추가한다.
   - artifact bundle image
   - publisher image
7. CI에서 test, publish, docker build, docker push 순서를 자동화한다.

## 현재 모듈 개편 매핑

| 현재 모듈 | 유지/변경 | 비고 |
| --- | --- | --- |
| `modules:error-handling` | 유지 | 필요 시 core/web 책임 분리 |
| `modules:time` | 유지 | core 의존만 허용 |
| `modules:jpa` | 유지 | JPA 공통 기반 |
| `modules:security` | 유지 | JPA 없는 보안 기반 |
| `modules:security-jpa` | 유지 | refresh token JPA 구현 |
| `modules:api-starter` | 유지 | 전체 API 편의 starter |
| `modules:core` | 추가 검토 | 공통 contract가 늘어날 때만 추가 |
| `modules:web` | 추가 검토 | starter의 web 책임이 커질 때 분리 |

## 단계별 실행 계획

### 1단계: 현재 모듈 책임 재정의

- 각 모듈의 공개 API와 내부 구현을 구분한다.
- `api`/`implementation` 의존성을 재검토한다.
- `security`가 JPA에 의존하지 않는지 확인한다.
- `jpa`가 Web/Security에 의존하지 않는지 확인한다.
- `api-starter`가 편의 starter 역할만 하도록 정리한다.

완료 기준:

- `./gradlew clean test publishToMavenLocal` 통과
- README에 모듈별 선택 사용 방식이 문서화됨

### 2단계: artifact 배포 기준 정리

- artifactId 목록 확정
- semantic versioning 기준 작성
- snapshot/release 배포 정책 작성
- Docker Hub image name과 tag 규칙 확정

완료 기준:

- 각 모듈의 publish artifact가 명확함
- 소비 프로젝트가 고정 버전으로 의존할 수 있음

### 3단계: Docker Hub 배포 방식 구현

- Dockerfile 작성
- Gradle build 산출물을 이미지에 포함하거나 publisher image로 구성
- Docker Hub push 절차 작성
- 필요한 secret 환경 변수 정의

완료 기준:

- `docker build`로 배포 이미지 생성 가능
- Docker Hub에 version tag로 push 가능
- 이미지 사용 방법이 문서화됨

### 4단계: 소비 프로젝트 검증

- 별도 샘플 모놀리식 프로젝트에서 `api-starter` 의존 검증
- 별도 샘플 MSA 서비스에서 개별 모듈 조합 검증
- H2 또는 Testcontainers로 최소 구동 확인

완료 기준:

- 모놀리식 샘플에서 swagger, validation, error handling, jpa, security 동작 확인
- MSA 샘플에서 필요한 모듈만 선택 의존 가능
- DB driver는 소비 프로젝트에서 직접 제공하는 구조 확인

### 5단계: 문서화

- README를 “공통 라이브러리 사용법” 중심으로 정리
- `docs/monolith-usage.md` 작성
- `docs/msa-usage.md` 작성
- Docker Hub 배포/사용 문서 작성
- 모듈별 설정 property 목록 작성

완료 기준:

- 새 프로젝트가 문서만 보고 의존성 추가와 기본 설정을 할 수 있음
- starter 사용과 개별 모듈 조합 사용이 모두 설명됨

## 테스트 전략

- 모듈 단위 테스트
- auto configuration 테스트
- `publishToMavenLocal` 후 샘플 프로젝트 의존 테스트
- Docker image build 테스트
- release artifact smoke test

필수 명령:

```bash
./gradlew clean test
./gradlew publishToMavenLocal
docker build -t dochiri/multi-module-artifacts:<version> .
```

## 결정 필요 사항

- Docker Hub를 artifact bundle image로 쓸지, publisher image로 쓸지
- 실제 Maven artifact를 어디에 publish할지
  - Maven Central
  - GitHub Packages
  - Nexus/Artifactory
  - Docker image 내부 Maven repo
- `modules:web`, `modules:core`를 지금 추가할지 나중에 추가할지
- 기존 artifactId를 유지할지, 새 이름으로 변경할지
- snapshot과 release version 정책
- CI/CD 도구
