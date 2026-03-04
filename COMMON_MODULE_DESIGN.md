# 공통 모듈 JAR 배포 설계

## 목표

JPA, Security 등 공통 모듈을 독립적인 JAR로 빌드하여, 새로운 프로젝트에서 **의존성 추가만으로** 별도 설정 없이 바로 동작하게 한다. 모든 프로젝트가 동일한 인프라 설정을 공유하여 일관성을 유지한다.

---

## 1. 프로젝트 구조

이 레포는 **공통 모듈만 관리**하는 전용 레포다. 테스트용 applications는 제거한다.

```
[Before]
multi-module/
├── applications/
│   ├── commerce-api/        ← 삭제
│   └── commerce-batch/      ← 삭제
└── modules/
    ├── jpa/
    └── security/

[After]
multi-module/
├── modules/
│   ├── jpa/                 → com.dochiri:dochiri-jpa JAR
│   └── security/            → com.dochiri:dochiri-security JAR
├── build.gradle
└── settings.gradle
```

### settings.gradle

```gradle
rootProject.name = 'multi-module'

include(
    'modules:jpa',
    'modules:security'
)
```

### build.gradle (루트)

```gradle
plugins {
    id 'io.spring.dependency-management' version '1.1.7'
}

allprojects {
    group = 'com.dochiri'
    version = '0.0.1-SNAPSHOT'

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply plugin: 'java-library'
    apply plugin: 'maven-publish'
    apply plugin: 'io.spring.dependency-management'

    dependencyManagement {
        imports {
            mavenBom "org.springframework.boot:spring-boot-dependencies:3.5.11"
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    dependencies {
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'

        testCompileOnly 'org.projectlombok:lombok'
        testAnnotationProcessor 'org.projectlombok:lombok'

        testImplementation 'org.springframework.boot:spring-boot-starter-test'
        testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    }

    tasks.named('test') {
        useJUnitPlatform()
    }

    // 공통 publishing 설정
    publishing {
        publications {
            mavenJava(MavenPublication) {
                from components.java
                groupId = 'com.dochiri'
            }
        }
        repositories {
            mavenLocal()
        }
    }
}
```

---

## 2. 배포 방식

| 방식 | 장점 | 단점 | 적합한 경우 |
|------|------|------|------------|
| **GitHub Packages** | GitHub 연동 간편, Private 지원, 무료 500MB | GitHub 계정 필요 | 팀/개인 프로젝트 |
| **로컬 Maven Repository** (`mavenLocal`) | 설정 간단, 네트워크 불필요 | 머신 간 공유 불가 | 1인 개발, 학습용 |
| **사설 Nexus/Artifactory** | 완전한 통제, 팀 공유 | 서버 운영 필요 | 회사/팀 프로젝트 |

---

## 3. 핵심 원리 — Auto Configuration

Spring Boot Starter가 의존성만 추가하면 자동으로 동작하는 이유는 Auto Configuration 때문이다.
우리 모듈도 같은 방식을 적용한다.

### 동작 원리

```
1. 프로젝트에서 dochiri-jpa JAR를 의존성에 추가
2. Spring Boot가 JAR 안의 AutoConfiguration.imports 파일을 읽음
3. 파일에 적힌 Configuration 클래스를 자동으로 Bean 등록
4. 끝. 사용하는 프로젝트에서 할 일 없음.
```

### 필요한 파일

각 모듈에 한 줄짜리 텍스트 파일 하나만 추가하면 된다.

**modules/jpa/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:**

```
com.dochiri.jpa.config.JpaConfig
```

**modules/security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:**

```
com.dochiri.security.config.SecurityAutoConfiguration
```

### 이게 없으면?

사용하는 프로젝트마다 아래처럼 설정을 직접 해야 한다:

```java
// 매 프로젝트마다 이걸 기억해서 넣어야 함
@SpringBootApplication(scanBasePackages = {"com.myproject", "com.dochiri"})
```

누군가 빠뜨리면 Bean이 안 뜨고, 프로젝트마다 설정이 달라지며, "일관성" 목표에 어긋난다.

---

## 4. 모듈별 구조

### 4-1. modules/jpa

현재 `JpaSupportConfig`에 이미 `@Configuration`, `@EnableJpaAuditing`이 있으므로 **Java 코드 변경 없이** imports 파일만 추가하면 된다.

```
modules/jpa/
├── src/main/java/com/dochiri/jpa/
│   ├── config/JpaSupportConfig.java      ← 기존 그대로
│   └── common/BaseEntity.java            ← 기존 그대로
├── src/main/resources/
│   └── META-INF/spring/
│       └── ...AutoConfiguration.imports  ← 추가
└── build.gradle
```

**build.gradle:**

```gradle
description = 'jpa-module'

dependencies {
    api 'org.springframework.boot:spring-boot-starter-data-jpa'
    api 'com.querydsl:querydsl-jpa:5.1.0:jakarta'

    annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            artifactId = 'dochiri-jpa'
        }
    }
}
```

### 4-2. modules/security

`AuthController`를 제거하고, `SecurityConfig`를 Auto Configuration으로 전환한다.

```
modules/security/
├── src/main/java/com/dochiri/security/
│   ├── config/
│   │   ├── SecurityAutoConfiguration.java   ← SecurityConfig에서 전환
│   │   ├── JwtProperties.java               ← 기존 그대로
│   │   └── CurrentUserAuditorAware.java     ← 기존 그대로
│   ├── jwt/
│   │   ├── JwtTokenProvider.java            ← 기존 그대로
│   │   └── JwtAuthenticationFilter.java     ← 기존 그대로
│   └── api/AuthController.java              ← 삭제 (각 프로젝트에서 구현)
├── src/main/resources/
│   └── META-INF/spring/
│       └── ...AutoConfiguration.imports     ← 추가
└── build.gradle
```

**SecurityAutoConfiguration.java:**

```java
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

`@ConditionalOnMissingBean` — "이 Bean이 이미 있으면 등록하지 마라"

- 프로젝트에서 `SecurityFilterChain`을 **안 만들면** → 모듈의 기본 설정 사용
- 프로젝트에서 `SecurityFilterChain`을 **직접 만들면** → 모듈 것은 무시됨

**build.gradle:**

```gradle
description = 'security-module'

dependencies {
    api 'org.springframework.boot:spring-boot-starter-security'

    implementation 'org.springframework.boot:spring-boot-autoconfigure'
    implementation 'org.springframework:spring-web'
    compileOnly 'org.springframework.data:spring-data-commons'
    compileOnly 'jakarta.servlet:jakarta.servlet-api'

    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            artifactId = 'dochiri-security'
        }
    }
}
```

---

## 5. 새 프로젝트에서 사용하는 방법

### 5-1. 의존성 추가 (이게 전부)

```gradle
repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation 'com.dochiri:dochiri-jpa:0.0.1-SNAPSHOT'
    implementation 'com.dochiri:dochiri-security:0.0.1-SNAPSHOT'
}
```

### 5-2. application.yml

```yaml
security:
  jwt:
    secret: my-project-secret-key-at-least-32-bytes-long
    access-token-expiration-seconds: 3600
```

### 5-3. 자동으로 제공되는 것

| 모듈 | 자동 제공 항목 |
|------|--------------|
| **dochiri-jpa** | BaseEntity(감사 필드 + Soft Delete), JPA Auditing, JPAQueryFactory Bean |
| **dochiri-security** | JWT 발급/검증, 인증 필터, Spring Security 기본 설정, BCryptPasswordEncoder |

### 5-4. 프로젝트별 커스터마이징이 필요할 때

Security 기본 설정을 바꾸고 싶으면 `SecurityFilterChain` Bean을 직접 정의한다. 모듈의 기본 설정은 자동으로 무시된다.

```java
@Configuration
public class MySecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

---

## 6. JAR 빌드 및 배포

```bash
# mavenLocal에 배포 (~/.m2/repository에 저장)
./gradlew :modules:jpa:publishToMavenLocal
./gradlew :modules:security:publishToMavenLocal

# GitHub Packages에 배포 (repositories에 GitHub Packages 설정 추가 필요)
./gradlew :modules:jpa:publish
./gradlew :modules:security:publish
```

---

## 7. GitHub Packages + GitHub Actions 자동 배포

### .github/workflows/publish-modules.yml

```yaml
name: Publish Common Modules

on:
  push:
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Publish JPA Module
        run: ./gradlew :modules:jpa:publish
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Publish Security Module
        run: ./gradlew :modules:security:publish
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 배포 흐름

```
코드 수정 → 태그 생성 (git tag v0.0.1) → push → GitHub Actions 자동 배포
                                                       ↓
                                              GitHub Packages에 JAR 등록
                                                       ↓
                                              다른 프로젝트에서 의존성으로 사용
```

---

## 8. 단계별 실행 계획

### Phase 1: 프로젝트 정리

1. `applications/` 디렉토리 삭제
2. `settings.gradle`에서 applications 모듈 제거
3. `build.gradle` 루트에서 `spring-boot` 플러그인 제거, `maven-publish` 공통 설정 추가

### Phase 2: 모듈 정리

1. security 모듈에서 `AuthController` 삭제
2. `SecurityConfig` → `SecurityAutoConfiguration`으로 전환
3. 각 모듈에 `AutoConfiguration.imports` 파일 추가

### Phase 3: 로컬 배포 검증

1. `publishToMavenLocal`로 JAR 배포
2. 별도 테스트 프로젝트 생성 → 의존성만 추가하여 동작 확인

### Phase 4: 원격 배포

1. GitHub Packages 설정
2. GitHub Actions CI/CD 파이프라인 구성
3. 태그 기반 자동 배포

---

## 9. 버전 관리

```
MAJOR.MINOR.PATCH

0.x.x  → 초기 개발 (현재)
1.0.0  → 안정화 릴리즈
1.1.0  → 하위 호환 기능 추가
2.0.0  → 하위 호환 깨지는 변경
```

- **SNAPSHOT**: 개발 중. `0.0.1-SNAPSHOT` → 매번 최신을 가져옴
- **Release**: 안정화 후. `0.0.1` → 불변, 캐싱됨

---

## 10. 주의사항

| 항목 | 설명 |
|------|------|
| **Spring Boot 버전 호환** | 모듈과 사용 프로젝트의 Spring Boot 메이저 버전이 일치해야 한다 |
| **일반 JAR로 빌드** | `java-library` 플러그인만 사용하면 `bootJar`가 아닌 일반 JAR로 자동 생성 |
| **transitive 의존성** | `api`로 선언한 의존성(spring-data-jpa 등)은 사용 프로젝트에 자동 전파 |
| **SNAPSHOT 캐싱** | Gradle이 24시간 캐싱. 즉시 반영 필요 시 `--refresh-dependencies` 사용 |
