# Starter 전환 계획

작성일: 2026-03-07  
대상 프로젝트: `multi-module`

## 1. 목표

- 새 프로젝트 시작 시 여러 모듈을 조합하지 않고, 팀 표준 구성을 starter 하나로 의존받게 한다.
- 내부 구현은 멀티 모듈 구조를 유지해 책임 분리와 테스트 단위를 보존한다.
- 외부 공개 계약은 "개별 모듈 선택"보다 "starter 중심"으로 단순화한다.

## 2. 핵심 결정

- 내부 구조:
  - `error-handling`, `time`, `jpa`, `security`는 유지
- 외부 공식 진입점:
  - 1차: `dochiri-api-starter`
  - 2차: 필요가 생기면 `dochiri-batch-starter`
- 권장 사용 방식:
  - 신규 API 서비스는 starter 의존을 기본 경로로 사용
  - 개별 모듈 직접 사용은 고급/예외 경로로 낮춘다

## 3. 왜 이 방향이 맞는가

- 현재 구조는 이미 완전 독립 라이브러리 세트보다 "서비스 공통 기반 세트"에 가깝다.
- `jpa`와 `security` 사이에 암묵적 결합이 있어, 개별 모듈 선택 사용을 공식 경로로 두면 문서/테스트/배포 계약 비용이 커진다.
- starter는 멀티 모듈의 반대 개념이 아니다.
  - 멀티 모듈: 내부 구현 분리
  - starter: 외부 소비 계약
- 장기적으로 중요한 것은 재사용성 자체보다 "새 프로젝트가 같은 방식으로 시작되도록 강제하는 것"이다.

## 4. 목표 구조

```text
multi-module/
├── modules/
│   ├── error-handling/
│   ├── jpa/
│   ├── security/
│   ├── time/
│   └── api-starter/
└── ...
```

### 예상 의존 관계

```text
api-starter
  ├── error-handling
  ├── jpa
  ├── security
  └── time
```

### 공개 계약 원칙

- 공식 지원:
  - `dochiri-api-starter`
- 제한적 지원:
  - `dochiri-error-handling`
  - `dochiri-time`
- 내부/예외 경로:
  - `dochiri-jpa`
  - `dochiri-security`

## 5. 왜 단일 `dochiri-starter`보다 `api-starter`가 우선인가

- 현재 모듈 조합은 사실상 API 서비스 전제에 가깝다.
- 배치, 워커, CLI까지 한 starter에 묶으면 불필요한 보안/웹/JPA 가정이 같이 따라붙는다.
- 우선 `api-starter` 하나로 표준 경로를 만든 뒤, 실제로 다른 서비스 패턴이 생기면 그때 `batch-starter`를 추가하는 편이 장기적으로 안정적이다.

## 6. 실행 단계

### Phase 0. 지원 정책 확정

- 팀 기준으로 다음 계약을 문서화한다.
  - 신규 API 서비스는 `dochiri-api-starter` 사용
  - 개별 모듈 직접 사용은 비권장
  - 지원 조합과 비지원 조합을 명시
- 결정해야 할 항목:
  - `jpa` 단독 사용을 계속 지원할지
  - `security` 단독 사용을 계속 지원할지
  - `error-handling`을 독립 라이브러리로 계속 유지할지
  - `time`을 독립 라이브러리로 계속 유지할지

### Phase 1. `api-starter` 모듈 추가

- `settings.gradle`에 `modules:api-starter` 추가
- `modules/api-starter/build.gradle` 추가
- `artifactId`: `dochiri-api-starter`
- 초기 구성 원칙:
  - 가능한 한 얇은 aggregator로 시작
  - 구현 코드는 최소화
  - `api project(':modules:error-handling')`
  - `api project(':modules:jpa')`
  - `api project(':modules:security')`
  - `api project(':modules:time')`
- 필요 시 starter 전용 설명용 `README` 또는 루트 문서 섹션 추가

### Phase 2. starter 기준으로 기존 모듈 계약 정리

- `security`
  - `spring-data-commons` 런타임 의존 문제 정리
  - `SecurityAuditAutoConfiguration` 조건부 로딩 여부 확정
- `jpa`
  - `AuditorAware` 부재 시 정책 결정
  - 선택지:
    - fail-fast
    - 기본 auditor 제공
    - starter 사용 시에만 지원
- `error-handling`
  - 공개 API에 노출되는 의존성을 `api` 기준으로 재분류
- `time`
  - `Clock` 오버라이드 가능성 검토
- `security`
  - `jjwt-api` 등 공개 시그니처 의존성 scope 재분류
  - Refresh 토큰 예외 계약 단일화

### Phase 3. 문서 재정렬

- `README.md`
  - "필요한 모듈만 선택 추가" 문구를 starter 중심 설명으로 변경
  - 신규 서비스 시작 예제를 `dochiri-api-starter` 기준으로 변경
  - 개별 모듈 사용은 별도 섹션으로 내린다
- `COMMON_MODULE_DESIGN.md`
  - 현재 구조 기준으로 동기화하거나 deprecated 처리
- `CODEX_REVIEW.md`
  - starter 도입 이후 해결/잔존 이슈 기준으로 후속 업데이트 가능

### Phase 4. 검증 체계 추가

- `api-starter` 컨텍스트 기동 테스트 추가
- 소비자 관점 smoke test 추가
  - 예: 샘플 Spring Boot 앱이 `dochiri-api-starter` 하나만으로 기동되는지 확인
- `publishToMavenLocal` 이후 샘플 소비자 프로젝트 컴파일 검증 추가
- 지원 조합별 검증 추가
  - `api-starter` 사용 시 성공
  - 비지원 조합 사용 시 명확히 실패하거나 문서와 일치

### Phase 5. 소비자 마이그레이션

- 기존 서비스가 `dochiri-error-handling`, `dochiri-time`, `dochiri-jpa`, `dochiri-security`를 직접 의존한다면:
  - 1차: `dochiri-api-starter` 추가
  - 2차: 중복되는 직접 의존 제거
  - 3차: 기동/보안/JPA auditing/시간 설정 회귀 확인
- 마이그레이션 가이드를 문서화한다.

## 7. 구현 우선순위

1. `api-starter` 모듈 추가
2. `security`의 런타임 의존/자동설정 조건 문제 정리
3. `jpa`의 `AuditorAware` 정책 확정
4. 공개 API 기준 dependency scope 정리
5. README 및 설계 문서 업데이트
6. 샘플 소비자 프로젝트 기반 검증 추가

## 8. 성공 기준

- 신규 API 서비스가 `dochiri-api-starter` 하나로 기본 인프라를 기동할 수 있다.
- 문서가 starter 중심 경로를 명확히 안내한다.
- 개별 모듈 직접 사용 여부와 지원 범위가 문서/테스트/배포 산출물과 일치한다.
- POM/런타임 클래스패스 기준으로 소비자 컴파일/기동 계약이 깨지지 않는다.

## 9. 비목표

- 지금 당장 모든 서비스 유형을 하나의 만능 starter로 통합하지 않는다.
- 내부 모듈을 없애고 단일 프로젝트 구조로 되돌리지 않는다.
- "모든 모듈 조합을 공식 지원"하는 목표를 유지하지 않는다.

## 10. 추천 결론

- 장기적으로 가장 현실적인 방향은 `멀티 모듈 유지 + starter를 공식 진입점으로 채택`이다.
- 시작은 `dochiri-api-starter` 하나로 충분하다.
- `batch-starter`는 실제 두 번째 사용 패턴이 생겼을 때 추가하는 것이 맞다.
