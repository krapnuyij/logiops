# 테스트 전략

## 1. 목적

테스트는 구현 존재 여부가 아니라 다음 위험을 검증한다.

- 재고 계산과 주문 상태 전이가 업무 규칙을 지키는가
- 주문, 재고, 이동 이력이 같은 트랜잭션으로 처리되는가
- API가 문서화된 정상·오류 계약을 지키는가
- 동시 요청에서 초과 예약이나 중복 처리가 발생하지 않는가
- Flyway 스키마와 JPA 매핑이 일치하는가
- H2에서 통과한 기능이 MSSQL에서도 동작하는가

## 2. 원칙

- 새 기능과 도메인 규칙에는 테스트를 함께 추가한다.
- 가장 작은 범위에서 검증하되 DB 트랜잭션과 잠금을 mock으로 대체하지 않는다.
- 테스트는 실행 순서와 기존 DB 상태에 의존하지 않아야 한다.
- 랜덤 값이 필요하면 테스트 클래스에서 명시적인 seed를 관리한다.
- 시간 비교가 필요한 도메인 코드에는 제어 가능한 `Clock` 사용을 검토한다.
- 단순 커버리지 수치보다 핵심 분기와 실패 시나리오를 우선한다.
- H2와 MSSQL의 결과를 구분해 보고한다.

## 3. 테스트 계층

### 3.1 도메인 단위 테스트

Spring Context와 DB 없이 Entity 및 값 검증을 테스트한다.

대상:

- SKU `strip()`, 허용 문자·길이 검증, `Locale.ROOT` 대문자 정규화
- 상품명 `strip()`과 1~100자 검증
- 가용재고 계산
- 양수가 아닌 입고·예약·출고·해제 수량 거부
- 가용재고 초과 예약 거부
- 예약재고 초과 출고·해제 거부
- 입고·예약·출고·해제 후 재고 수치
- `RESERVED`에서 `SHIPPED`, `CANCELLED` 전이
- 최종 상태 재처리 거부

### 3.2 Repository 테스트

`@DataJpaTest` 또는 동등한 JPA slice를 사용하되 Flyway 적용 방식과 충돌하지 않도록 실제 구성을 확인한다.

대상:

- Entity 매핑과 제약조건
- SKU unique 제약
- 상품별 Inventory unique 제약
- 주문 내 상품 unique 제약
- 재고 음수 및 현재재고 미만 조건 차단
- Repository 잠금 쿼리
- 이동 이력의 productId·orderId·type 개별·복수 필터 조합과 고정 정렬
- 페이지 조회

### 3.3 Service 통합 테스트

실제 Spring Context, JPA, 테스트 DB를 사용한다.

대상:

- 상품 등록과 0 재고 생성의 원자성
- 입고와 이동 이력 저장
- 이동 이력 저장 실패 시 재고 변경 rollback
- 복수 상품 주문의 전체 예약 성공
- 일부 상품 재고 부족 시 주문·예약·이력 전체 롤백
- 주문 이력 저장 실패 시 주문·예약 전체 rollback
- 출고 완료와 재고·이력 변경
- 주문 취소와 예약 해제·이력 변경
- 이미 처리된 주문의 재처리 실패와 상태 불변

트랜잭션 경계를 검증할 때 테스트 메서드 자체의 자동 rollback이 실제 서비스 커밋 동작을 가리지 않는지 확인한다.

### 3.4 API 테스트

MockMvc를 기본으로 사용한다.

대상:

- 요청 JSON 역직렬화
- Bean Validation
- SKU 도메인 검증 실패
- `page=0`, `size=1`, `size=100` 경계값과 범위 밖 요청
- `size=abc` 타입 변환 실패
- HTTP 201과 `Location` 헤더
- 조회 응답과 페이지 형식
- 400, 404, 409 오류 매핑
- `ProblemDetail`의 `errorCode` 확장 필드
- 필드 검증 오류의 `fieldErrors` 구조와 고정 정렬
- JSON 역직렬화 실패의 `MALFORMED_REQUEST` 구분
- 내부 예외 정보 비노출
- API별 OpenAPI 오류 응답과 공통 오류 스키마

400 응답은 다음 네 오류 발생 경로를 대상으로 한다.

- 요청 DTO Bean Validation
- `page`, `size` 메서드 파라미터 제약
- 요청 파라미터 타입 변환 실패
- SKU 도메인 검증 실패

현재 Controller 시그니처에서 실제 확인한 Spring MVC 예외는 각각 `MethodArgumentNotValidException`, `HandlerMethodValidationException`, `MethodArgumentTypeMismatchException`이다. SKU 실패는 도메인 `ValidationException` 경로를 사용한다. Stage 6에서 네 경로가 모두 `400 VALIDATION_FAILED`와 가능한 경우 필드 오류로 변환됨을 검증했다. Controller 시그니처가 바뀌면 실제 예외 유형을 다시 확인한다. Spring MVC 내장 메서드 검증을 사용하며 Controller 클래스 수준 `@Validated`는 사용하지 않는다.

깨진 JSON 또는 JSON 본문 타입을 역직렬화할 수 없는 `HttpMessageNotReadableException` 경로는 위 네 가지 `VALIDATION_FAILED` 통일 대상과 구분한다. 이 경로가 `400 MALFORMED_REQUEST`를 반환함을 별도로 검증했다.

예상하지 못한 예외는 내부 메시지와 예외 클래스가 없는 안전한 `500 INTERNAL_SERVER_ERROR` 응답인지 검증한다. 테스트 DB에서 주문 항목과 예약재고의 관계를 의도적으로 어긋나게 만들어 Aggregate 간 정합성 오류의 500 응답과 트랜잭션 rollback을 확인한다. 포괄 예외 처리기가 Spring MVC의 미등록 경로 404와 지원하지 않는 메서드 405를 500으로 바꾸지 않는지도 회귀 테스트한다.

### 3.5 애플리케이션 스모크 테스트

대상:

- Spring Context 기동
- `/actuator/health`
- `/v3/api-docs`
- Swagger UI 리소스
- 설정 프로필 로딩

Stage 1에서 실제 애플리케이션 실행과 HTTP 호출을 함께 수행한다.

### 3.6 동시성 테스트

각 작업이 별도 트랜잭션과 DB 커넥션을 사용하도록 구성한다. `ExecutorService`, 시작 barrier, 완료 latch 또는 동등한 동기화 도구로 경쟁 시점을 맞춘다.

필수 시나리오:

1. `sku-001`, `SKU-001`처럼 정규화 결과가 같아지는 입력으로 동시에 등록한다.
2. 동일 상품에 여러 입고를 동시에 실행한다.
3. 동일 상품에 입고와 예약을 동시에 실행한다.
4. 가용재고와 같은 총수량을 여러 요청이 동시에 예약한다.
5. 가용재고보다 많은 합계 수량을 여러 요청이 동시에 예약한다.
6. 동일 주문을 두 요청이 동시에 출고 완료한다.
7. 동일 주문에 출고와 취소를 동시에 요청한다.
8. 서로 다른 항목 순서의 복수 상품 주문을 동시에 생성한다.

검증 항목:

- 성공 요청 수
- 최종 현재재고, 예약재고, 가용재고
- 주문 상태와 개수
- 이동 이력의 유형과 개수
- 음수 재고 또는 초과 예약 부재
- 테스트 timeout 내 종료 여부

H2 동시성 테스트는 애플리케이션 로직과 기본 동시성에 대한 회귀 검증이다. 잠금 의미와 deadlock 특성이 MSSQL과 같다고 간주하지 않는다. 비관적 잠금과 트랜잭션 동작은 Stage 9에서 실제 MSSQL로 별도 검증했다.

Stage 7에서는 각 작업을 별도 스레드의 Service 호출로 실행해 독립 트랜잭션과 DB 연결을 사용했다. 시작 latch와 결과·테스트 timeout을 함께 사용하고 승자 실행 순서가 아닌 최종 상태를 검증했다. 동일 SKU 등록과 동시 입고, 초과 예약은 기존 테스트를 유지했으며 다음 시나리오를 추가했다.

- 동일 상품의 입고와 예약 경쟁
- 합계가 가용재고와 같은 복수 주문의 동시 예약
- 동일 주문의 동시 출고
- 동일 주문의 출고와 취소 경쟁
- 상품 입력 순서가 반대인 복수 상품 주문의 동시 생성

신규 시나리오는 연속 3회 실행에서 timeout 없이 통과했다. 전체 H2 테스트에서도 초과 예약, 음수 재고, 중복 최종 처리, 주문·재고·이력 불일치가 관찰되지 않았다. 이 결과는 Stage 9 MSSQL 검증을 대체하지 않는다.

### 3.7 MSSQL 호환성 테스트

Stage 9에서는 GitHub Actions `ubuntu-24.04` x86-64 runner에서 SQL Server 2022 서비스 컨테이너를 사용한다. 로컬 컨테이너 수명주기를 관리하기 위한 Testcontainers 의존성은 추가하지 않는다.

MSSQL 테스트에는 JUnit `mssql` tag를 지정하고 별도 Gradle `mssqlTest` task로 실행한다. 일반 `test`와 `check`는 이 tag를 제외해 Apple Silicon 로컬 환경에서도 MSSQL 접속 없이 H2 회귀 검증을 수행한다. Apple Silicon 에뮬레이션 결과는 공식 지원 환경의 검증 결과로 간주하지 않는다.

대상:

- 빈 DB에 전체 Flyway 마이그레이션 적용
- JPA `ddl-auto=validate`
- 상품·입고·주문·출고·취소 핵심 흐름
- unique/check/FK 제약
- 비관적 잠금과 동시 예약
- 시간 타입과 ID 생성 전략
- 한국어 상품명과 nationalized 문자열 왕복 저장
- 초과 동시 예약, 동일 주문 출고·취소 경쟁, 역순 다중 상품 잠금

CI는 `SELECT @@VERSION`으로 실제 SQL Server 엔진을 확인하고 Flyway V1~V5 적용 개수와 주요 column type을 검증한다. UNIQUE·CHECK·FK는 JDBC로 위반을 유도해 DB가 거부하는지 확인한다. 동시성 테스트는 별도 스레드와 독립 트랜잭션, 시작 latch와 timeout을 사용하며 승자 순서가 아니라 최종 상태를 검증한다.

MSSQL 컨테이너의 라이선스 동의는 워크플로 설정에서만 처리한다. 관리자 비밀번호는 GitHub Actions의 `MSSQL_SA_PASSWORD` secret으로 주입하며 저장소에 기록하지 않는다. JDBC와 Flyway 관련 의존성은 Spring Boot BOM 관리 버전을 우선하고, 실제 호환성 문제가 확인된 경우에만 개별 버전 고정을 검토한다.

Stage 9 최종 결과는 다음과 같이 구분한다.

- H2: 로컬 `./gradlew clean check --no-daemon`에서 테스트 107건이 통과했다. 이는 애플리케이션 로직과 H2 기반 회귀 검증 결과이다.
- MSSQL: GitHub Actions x86-64 SQL Server 2022에서 `mssqlTest`가 통과했다. 빈 DB migration, JPA validation, 핵심 흐름·DB 제약과 비관적 잠금 동시성 시나리오를 실제 MSSQL에서 확인한 결과이다.
- vendor 차이: V5의 새 컬럼 참조는 SQL Server batch 경계를 위해 `GO`로 분리했고, `Instant`는 Hibernate SQL Server dialect가 요구하는 `DATETIMEOFFSET(7)`로 정합화했다.
- 환경 제약: Apple Silicon 로컬 에뮬레이션은 위 MSSQL 결과의 대체 근거로 사용하지 않는다.

### 3.8 컨테이너 실행 테스트

Stage 8에서는 다음 항목을 실제 Docker 데몬에서 검증한다.

- Compose 설정 해석과 이미지 빌드
- 컨테이너 health 상태와 Actuator `UP`
- OpenAPI JSON과 Swagger UI 접근
- 상품 등록과 초기 재고 조회
- Java 21 런타임과 비루트 사용자
- 최종 이미지의 소스·Gradle 빌드 도구 제외
- 검증 후 프로젝트 컨테이너와 네트워크 정리

Docker 검증은 local 프로필과 인메모리 H2를 사용한다. 컨테이너 실행 성공을 MSSQL 호환성 검증으로 간주하지 않는다.

## 4. 단계별 테스트 범위

| 단계 | 필수 검증 |
|---|---|
| Stage 1 | Gradle test, Context, health, OpenAPI, Swagger UI |
| Stage 2 | Product 정규화·동시 SKU·페이지 검증·Repository·API 테스트, V1 migration |
| Stage 3 | Inventory 도메인·DB 제약, 신규·기존 상품 재고 초기화, 입고·이력 원자성, 동시 입고, 필터 조회, V2/V3 migration |
| Stage 4 | 복수 항목 예약, 부족 재고·이력 실패 rollback, 주문 API, 주문 ID 이력 필터, 기본 동시 예약 |
| Stage 5 | 출고·취소 상태 전이와 트랜잭션 |
| Stage 6 | 전체 오류 코드, `ApiProblemDetail.errorCode`·`fieldErrors`, 안전한 500, OpenAPI 오류 계약 |
| Stage 7 | 잠금 쿼리와 병렬 정합성 테스트 |
| Stage 8 | 이미지 빌드, 컨테이너 기동, health와 Swagger 접근 |
| Stage 9 | MSSQL migration, 핵심 통합·동시성 테스트 |
| Stage 10 | README의 명령을 깨끗한 환경에서 재검증 |

## 5. 테스트 데이터 관리

- 공통 생성 로직은 작은 Test Fixture 또는 Object Mother로 관리한다.
- 실제 업무 규칙을 숨기는 과도한 범용 builder는 만들지 않는다.
- 각 테스트는 필요한 상품과 재고를 직접 준비하고 의도를 드러낸다.
- 테스트 사이에 PK 값이나 실행 순서를 공유하지 않는다.
- 운영용 seed data는 제공하지 않는다.

## 6. 실행 명령

JDK 21과 Gradle Wrapper를 사용해 다음 명령을 품질 게이트로 실행한다.

```bash
./gradlew test
./gradlew check
```

단계별 실제 실행 결과와 테스트 수는 `CURRENT_STAGE.md`에 기록한다.

## 7. 완료 보고 형식

각 단계에서 다음을 보고한다.

- 실행한 명령
- 통과·실패·건너뜀 테스트 수
- 실패 원인과 영향 범위
- 사용한 DB와 프로필
- 실행하지 못한 테스트와 이유
- 다음 단계에서 필요한 추가 검증
