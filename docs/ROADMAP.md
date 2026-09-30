# 개발 로드맵

## 운영 원칙

- 한 번에 한 단계만 구현한다.
- 각 단계 시작 전 파일, 의존성, 명령, 검증 계획을 제시한다.
- 완료 조건을 실제로 검증한 뒤 다음 단계로 이동한다.
- 후속 단계 요구사항을 미리 구현하지 않는다.
- 아직 충족하지 못한 완료 조건은 `CURRENT_STAGE.md`에 남긴다.

## 문서 단계 — 완료

### 범위

- 프로젝트·도메인·API·아키텍처·테스트 명세
- 단계별 로드맵과 의사결정 기록
- 저장소 작업 규칙

### 완료 조건

- 지정된 문서가 모두 존재한다.
- 문서 링크와 핵심 용어가 일치한다.
- 미확정 사항이 확정된 것처럼 기록되지 않는다.
- 문서의 역할과 내용이 검토 기준에 부합한다.

## Stage 1 — 프로젝트 스캐폴딩 완료

### 범위

- Spring Boot 4.1.1 프로젝트 생성
- Java 21 toolchain과 Gradle Wrapper 8.14.3
- 기본 설정과 local/test 프로필 분리
- Spring Web, JPA, Validation, Actuator, H2, springdoc-openapi-starter-webmvc-ui 3.1.1 구성
- `ddl-auto=none`
- Context, health, OpenAPI, Swagger UI 검증
- README와 `CURRENT_STAGE.md` 갱신

### 제외

- Entity, Repository, 업무 API
- Flyway migration
- Dockerfile

### 완료 조건

- `./gradlew test`가 통과한다.
- 애플리케이션이 실제로 기동한다.
- health, OpenAPI, Swagger UI 경로를 HTTP로 확인한다.
- 실행 방법이 README와 일치한다.

## Stage 2 — 상품 등록과 조회 완료

### 범위

- Flyway 도입
- `V1__create_product_table.sql`
- Product Entity, Repository, Service, Controller, DTO
- SKU `strip()`·문자셋·길이 검증과 `Locale.ROOT` 대문자 정규화
- 상품명 `strip()`과 1~100자 검증
- 상품 등록, 단건 조회, 페이지 목록
- `page`, `size`를 명시적 요청 파라미터로 검증하고 `PageRequest` 생성
- Spring MVC 내장 메서드 검증 사용, Controller 클래스 수준 `@Validated` 미사용
- `ddl-auto=validate`

### 완료 조건

- 빈 H2 DB에 V1 마이그레이션이 적용된다.
- SKU unique 제약과 입력 검증이 동작한다.
- 정규화 결과가 같은 SKU의 동시 등록에서 정확히 한 요청만 성공한다.
- 페이지 경계값과 잘못된 타입·범위 요청이 API 계약과 일치한다.
- 상품 단위·Repository·API 테스트가 통과한다.
- 아직 Inventory 기능이 없음을 문서에 명시한다.

## Stage 3 — 입고와 재고 이동 이력 완료

### 범위

- Inventory와 StockMovement 스키마 및 Entity
- 기존 Product의 0 Inventory backfill
- 이후 상품 등록 시 0 Inventory 동시 생성
- 입고와 재고 조회
- 상품·유형별 이동 이력 조회

### 완료 조건

- 현재·예약·가용재고 계산 테스트가 통과한다.
- 입고와 `RECEIPT` 이력이 같은 트랜잭션으로 저장된다.
- 재고 DB 제약조건을 검증한다.
- 기존 상품과 신규 상품 모두 Inventory를 가진다.

## Stage 4 — 출고 주문 생성과 재고 예약 완료

### 범위

- OutboundOrder와 OutboundOrderItem 스키마 및 Entity
- 복수 상품 주문 생성
- 가용재고 검증과 예약
- `RESERVATION` 이동 이력
- 주문 조회

### 완료 조건

- 성공 주문이 모든 상품 재고를 예약한다.
- 한 상품이라도 부족하면 전체 작업이 롤백된다.
- 중복 상품과 잘못된 수량이 거부된다.
- 주문 생성 후 상태가 `RESERVED`이다.

동시성 안전성은 Stage 7 완료 전까지 최종 보장으로 표시하지 않는다.

## Stage 5 — 출고 완료와 주문 취소 완료

### 범위

- `RESERVED` 주문 출고 완료
- `RESERVED` 주문 취소
- `SHIPMENT`, `RESERVATION_RELEASE` 이력
- 최종 상태 재처리 차단

### 완료 조건

- 상태 전이별 재고 수치가 도메인 명세와 일치한다.
- 상태·재고·이력이 한 트랜잭션으로 처리된다.
- 출고·취소 재처리 테스트가 통과한다.

## Stage 6 — 예외 처리와 API 응답 통일 완료

### 범위

- `ProblemDetail` 기반 전역 오류 처리
- `errorCode`와 필드 검증 응답
- 400, 404, 409 계약 통일
- Stage 2에서 확인한 DTO 검증, 메서드 파라미터 검증, 타입 변환 실패, SKU 도메인 검증 경로를 `400 VALIDATION_FAILED`로 통일
- JSON 역직렬화 실패를 `400 MALFORMED_REQUEST`로 구분
- Aggregate 간 예약재고 불일치와 예상하지 못한 예외의 안전한 500 처리
- OpenAPI 오류 응답 문서화

### 완료 조건

- API 명세의 오류 코드가 테스트로 검증된다.
- 내부 예외와 SQL 정보가 응답에 노출되지 않는다.
- 기존 정상 흐름 테스트가 계속 통과한다.

## Stage 7 — 동시성 제어와 재고 정합성 완료

### 범위

- Inventory와 OutboundOrder 비관적 잠금
- 복수 재고 잠금 순서 고정
- 동시 예약, 출고, 취소 테스트
- 동일 상품 입고·예약 경쟁과 가용재고 경계값 예약 테스트
- 상품 입력 순서가 반대인 복수 상품 주문 경쟁 테스트
- timeout과 deadlock 관찰

### 완료 조건

- H2 병렬 테스트에서 초과 예약과 음수 재고가 없다.
- 동일 주문은 한 번만 최종 처리된다.
- 복수 상품 경쟁 테스트가 timeout 없이 끝난다.
- H2 한계를 문서에 유지한다.

## Stage 8 — Docker 실행환경 구현 완료

### 범위

- 멀티스테이지 Dockerfile
- `.dockerignore`
- 애플리케이션 Compose 구성
- 컨테이너 실행 문서

### 완료 조건

- 이미지가 실제로 빌드된다.
- 컨테이너 health가 정상이다.
- Swagger UI와 기본 API에 접근할 수 있다.
- 이미지에 소스·캐시·비밀정보가 불필요하게 포함되지 않는다.

## Stage 9 — MSSQL 연동 및 호환성 검증

현재 상태: 완료 및 검증. 프로필, DB별 migration, 전용 테스트와 x86-64 GitHub Actions 워크플로를 구현하고 실제 SQL Server 2022에서 검증했다.

### 범위

- MSSQL JDBC 드라이버와 프로필
- SQL Server 2022 이미지와 GitHub Actions x86-64 CI 실행 방식
- 공식 지원 조건에 맞는 x86-64 Linux 환경에서 MSSQL 기동 가능 여부 확인
- H2와 MSSQL 전용 Flyway 스크립트 분리
- JDBC와 Flyway는 Spring Boot BOM 관리 버전을 우선하고, 확인된 호환성 문제가 있을 때만 개별 버전 고정 검토
- 핵심 통합·동시성 테스트 재실행

### 완료 조건

- 빈 MSSQL DB에 전체 마이그레이션이 적용된다.
- JPA validation이 통과한다.
- 핵심 업무 흐름이 MSSQL에서 통과한다.
- JPA 매핑, 마이그레이션, 비관적 잠금과 트랜잭션 결과를 실제 MSSQL 실행 결과로 기록한다.
- H2와 MSSQL 차이와 대응을 의사결정 문서에 남긴다.

검증 과정에서 MSSQL V5의 새 컬럼 참조를 `GO`로 batch 분리했고, Hibernate SQL Server dialect에 맞춰 `Instant` 컬럼을 `DATETIMEOFFSET(7)`로 정정했다. 최종 x86-64 GitHub Actions 실행에서 위 완료 조건을 모두 확인했다. H2 결과와 MSSQL 결과는 서로 대체하지 않고 별도로 관리한다.

## Stage 10 — README와 포트폴리오 정리

현재 상태: 완료 및 검증. 검증된 구현·설계·문제 해결 근거를 README 중심으로 정리하고 깨끗한 임시 작업 디렉터리에서 실행 절차를 재현했다.

### 범위

- 검증된 실행 방법과 API 예시
- 아키텍처와 트랜잭션 설명
- 동시성 문제와 해결 과정
- 테스트 결과 요약
- 향후 확장과 한계

### 완료 조건

- 깨끗한 환경에서 README 절차를 재현한다.
- 구현하지 않은 기능을 제거하거나 향후 계획으로 명확히 표시한다.
- 실제 테스트 결과만 사용한다.
- 프로젝트 설명이 코드와 문서에 일치한다.

임시 작업 디렉터리에서 H2 테스트 107건, local 프로필의 전체 API 예제 흐름과 Docker 빌드·기동 절차를 다시 검증했다. H2와 MSSQL 결과, Apple Silicon MSSQL 제약, V5 batch와 `DATETIMEOFFSET(7)` 정정 이력을 구분해 유지한다. PR #1 병합 후 main push로 시작된 GitHub Actions 실행 [36712963102](https://github.com/krapnuyij/logiops/actions/runs/36712963102)에서도 H2와 MSSQL 전체 검증이 성공했다.

## Stage 11 — 내장 데모 UI

현재 상태: 완료 및 검증. 기존 REST API를 변경하지 않고 Spring Boot 실행 JAR에 포트폴리오 시연용 정적 UI를 포함하고 실제 Chrome에서 핵심 업무 흐름을 검증했다.

### 범위

- Vanilla HTML, CSS, JavaScript 기반 단일 페이지
- 상품 등록·조회와 상품별 입고·재고 조회
- 복수 상품 출고 주문 생성과 주문 ID 조회
- 예약 주문의 출고·취소와 재고 이동 이력 조회
- `ProblemDetail` 오류 표시와 기본 반응형 레이아웃

### 완료 조건

- 정적 리소스 스모크 테스트와 기존 H2 회귀 테스트가 통과한다.
- Docker 이미지에서 `/`, health, OpenAPI와 Swagger UI에 접근할 수 있다.
- 실제 브라우저에서 등록·입고·예약·출고·취소·이력·오류 흐름을 확인한다.
- 별도 프론트엔드 빌드환경이나 UI 전용 백엔드 API가 추가되지 않는다.
