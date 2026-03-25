# Hexagonal User Department Sample

`User`, `Department` 도메인을 가진 실행 가능한 hexagonal 샘플 프로젝트다.

## 구조

- `domain`: 순수 도메인 모델
- `application.port.in`: 유스케이스 입력 포트
- `application.port.out`: 외부 의존 포트
- `application.service`: 유스케이스 구현
- `adapter.in.web`: REST API 어댑터
- `adapter.out.persistence`: JPA 어댑터
- `support.error`: 공통 에러 응답 활성화

## 실행

레포 루트에서 실행한다.

```bash
./gradlew -p samples/hexagonal-user-department run
```

## 테스트

```bash
./gradlew -p samples/hexagonal-user-department test
```

## 주요 API

### 부서 생성

```bash
curl -X POST http://localhost:8080/api/public/departments \
  -H 'Content-Type: application/json' \
  -d '{"name":"Platform","description":"공통 플랫폼 부서"}'
```

### 사용자 생성

```bash
curl -X POST http://localhost:8080/api/public/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Song","email":"song@example.com","departmentId":1}'
```

### 부서별 사용자 조회

```bash
curl http://localhost:8080/api/public/departments/1/users
```
