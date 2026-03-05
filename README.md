# Multi-Module Project (Common Libraries)

이 프로젝트는 **Dochiri** 서비스 생태계에서 사용되는 공통 기능을 모듈화하여 관리하는 저장소입니다.  
각 모듈은 독립적인 JAR 라이브러리로 빌드되어 다른 애플리케이션(API, Batch 등)에서 의존성으로 추가하여 사용됩니다.

## 🛠 Tech Stack

- **Java**: 25
- **Build Tool**: Gradle
- **Framework**: Spring Boot 4.0.3 (Dependency Management)
- **ORM**: JPA / QueryDSL 5.1.0
- **Security**: Spring Security / JJWT 0.12.6

## 📂 Module Structure

이 프로젝트는 공통 모듈만 관리하며, 실제 비즈니스 로직을 수행하는 애플리케이션은 포함하지 않습니다.

| 모듈명 | 경로 | 설명 | 주요 의존성 |
|:---:|:---|:---|:---|
| **common** | `modules/common` | 전역 유틸리티 및 공통 설정 (Clock 등) | `spring-context` |
| **jpa** | `modules/jpa` | JPA 설정, BaseEntity(Auditing), QueryDSL 설정 | `common`, `spring-data-jpa`, `querydsl` |
| **security** | `modules/security` | JWT 인증/인가, 보안 설정, 토큰 Provider | `common`, `spring-security`, `jjwt` |

## 🚀 Getting Started

### 1. Build Modules

루트 디렉토리에서 전체 모듈을 빌드합니다.

```bash
./gradlew clean build
```

### 2. Publish to Local Maven (로컬 개발용)

로컬 환경에서 다른 프로젝트가 이 모듈들을 사용할 수 있도록 로컬 Maven 저장소(`~/.m2/repository`)에 배포합니다.

```bash
# 모든 모듈을 로컬 maven 저장소에 배포
./gradlew publishToMavenLocal
```

## 📦 How to Use (in other projects)

다른 Spring Boot 프로젝트에서 이 모듈을 사용하려면 `build.gradle`에 아래와 같이 추가합니다.

### settings.gradle

```gradle
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal() // 로컬 빌드된 스냅샷 버전을 가져오기 위해 필요
    }
}
```

### build.gradle

```gradle
dependencies {
    // 필요한 모듈만 선택하여 추가
    implementation 'com.dochiri:dochiri-jpa:0.0.1-SNAPSHOT'
    implementation 'com.dochiri:dochiri-security:0.0.1-SNAPSHOT'
}
```

## ⚙️ Module Details

### JPA Module (`dochiri-jpa`)
- **BaseEntity**: 생성일, 수정일, 생성자, 수정자를 자동으로 관리하는 매핑 상위 클래스를 제공합니다.
- **QueryDSL**: 설정이 완료된 상태로 제공되어, 별도의 QueryDSL 설정 없이 바로 사용 가능합니다.

### Security Module (`dochiri-security`)
- **JWT**: `JwtTokenProvider`, `JwtAuthenticationFilter`를 통해 토큰 기반 인증을 제공합니다.
- **SecurityConfig**: 기본 보안 설정을 제공하며, 필요 시 사용자 정의 가능합니다.

---
**Note**: 상세한 설계 의도와 배포 전략은 `COMMON_MODULE_DESIGN.md` 문서를 참고하세요.
