# 현재 작업 단계

- 마지막 갱신일: 2026-09-30
- 현재 단계: Stage 10 완료, MVP 완료
- 애플리케이션 구현 상태: MVP 핵심 물류 흐름, H2·MSSQL 검증, Docker 실행환경과 포트폴리오 문서 정리 완료

## 완료 항목

- 초기 요구사항 분석과 개발 계획 수립
- 전역 `AGENTS.md`, `CLAUDE.md`와 프로젝트 규칙의 충돌 검토
- Git 저장소 초기화
- 프로젝트 로컬 `AGENTS.md`, `CLAUDE.md` 작성
- 요구사항, 아키텍처, 도메인, API, 테스트, 로드맵, 의사결정 문서 작성
- Apple Silicon arm64 환경에 Temurin JDK 21 설치 및 실행 검증
- Spring Boot 4.1.1, Java 21, Gradle Wrapper 8.14.3 기반 프로젝트 스캐폴딩
- local/test 프로필과 H2 데이터소스 구성
- Actuator health, OpenAPI, Swagger UI 구성과 스모크 테스트
- README 로컬 실행 절차 작성
- Flyway V1 상품 테이블 마이그레이션과 JPA `ddl-auto=validate` 적용
- Product Entity, Repository, Service, Controller와 요청·응답 DTO 구현
- SKU `strip()`·정규식·`Locale.ROOT` 정규화와 상품명 검증 구현
- 상품 등록, 단건 조회, ID 오름차순 페이지 목록 구현
- `ProblemDetail.errorCode` 기반 상품 API 400·404·409 응답 구현
- 정규화 결과가 같은 SKU의 동시 등록 검증
- Flyway V2 재고 테이블과 기존 상품 0 재고 backfill 마이그레이션
- Flyway V3 재고 이동 이력 테이블, 제약조건, 조회 인덱스 마이그레이션
- Inventory와 StockMovement Entity, Repository, Service, Controller, DTO 구현
- 신규 상품과 0 재고의 동일 트랜잭션 생성
- 비관적 쓰기 잠금을 사용한 입고와 `RECEIPT` 이동 이력의 동일 트랜잭션 저장
- 현재·예약·가용재고 조회와 상품·유형별 이동 이력 페이지 조회
- 재고 DB 제약, backfill, rollback, H2 동시 입고 테스트
- Flyway V4 출고 주문·주문 항목 테이블과 V5 재고 이동 이력 주문 FK 마이그레이션
- OutboundOrder와 OutboundOrderItem Entity, Repository, Service, Controller, DTO 구현
- 복수 상품 ID 오름차순 비관적 잠금과 가용재고 검증
- 주문·주문 항목·예약재고·`RESERVATION` 이력의 동일 트랜잭션 처리
- 출고 주문 생성·단건 조회와 재고 이동 이력 `orderId` 필터 구현
- 주문 입력·DB 제약, 부족 재고·이력 실패 rollback, H2 기본 동시 예약 테스트
- OutboundOrder 상태 전이와 주문 루트 비관적 쓰기 잠금 구현
- 상품 ID 오름차순 Inventory 잠금을 사용한 출고 완료·주문 취소 구현
- `SHIPMENT`, `RESERVATION_RELEASE` 이동 이력과 주문·재고·이력 단일 트랜잭션 처리
- 최종 상태 재처리와 교차 처리 `409 INVALID_ORDER_STATE` 차단
- 출고·취소 상태 시각 DB 제약, 이력 실패 rollback, API 계약 테스트
- `ApiProblemDetail`과 `ApiFieldError` 기반 공통 오류 응답 구현
- DTO·메서드 파라미터·타입 변환·도메인 검증의 `400 VALIDATION_FAILED` 응답 통일
- JSON 역직렬화 실패의 `400 MALFORMED_REQUEST` 분리
- 404·409 도메인 오류와 안전한 `500 INTERNAL_SERVER_ERROR` 응답 구현
- 예약재고와 주문 항목의 Aggregate 간 정합성 오류를 내부 서버 오류로 분류
- API별 OpenAPI 오류 응답과 공통 오류 스키마 문서화
- 입고·예약 경쟁과 가용재고 경계값 동시 예약 테스트 구현
- 동일 주문 동시 출고와 출고·취소 경쟁 테스트 구현
- 복수 상품의 반대 입력 순서 주문에 대한 고정 잠금 순서와 timeout 검증
- H2 병렬 테스트에서 주문·재고·이력 정합성과 초과 예약·중복 최종 처리 부재 확인
- Temurin Java 21 JDK·JRE 멀티스테이지 Dockerfile 구현
- 비루트 런타임 사용자와 Actuator 기반 Docker healthcheck 구성
- 로컬 H2 애플리케이션용 Compose 구성과 호스트 포트 재정의 지원
- 빌드 컨텍스트에서 소스 외 불필요한 파일과 비밀정보 후보를 제외하는 `.dockerignore` 구성
- Spring Boot BOM 기반 MSSQL JDBC와 Flyway SQL Server 모듈 추가
- H2와 MSSQL Flyway V1~V5 migration 분리
- 환경변수 기반 MSSQL 프로필과 `NVARCHAR` nationalized 문자열 매핑 구성
- MSSQL 핵심 흐름·DB 제약·동시성 전용 테스트 구현
- SQL Server 2022 서비스 컨테이너를 사용하는 x86-64 GitHub Actions 워크플로 구현
- 공통 UTC Clock의 DB 호환 마이크로초 정밀도 적용과 결정적 단위 테스트 구현
- MSSQL V5 migration의 DDL batch 경계 분리와 `DATETIMEOFFSET(7)` 시간 매핑 정합화
- x86-64 SQL Server 2022에서 migration, JPA validation, 핵심 흐름·DB 제약·동시성 테스트 완료
- README를 문제·업무 흐름·아키텍처·동시성·DB 차이·검증 근거 중심의 포트폴리오 진입점으로 정리
- local H2에서 처음부터 재현 가능한 상품·입고·출고·취소 API 실행 예시 정리
- API 명세를 구현·테스트 완료 상태로 갱신하고 과거 단계 표현 제거
- MSSQL Actions가 기능 브랜치와 main push에서 모두 실행되도록 trigger 범위 정리
- 빌드 산출물과 프로젝트 캐시가 없는 임시 작업 디렉터리에서 Gradle·local·Docker 절차 재검증
- PR #1을 merge commit 방식으로 main에 병합하고 main push MSSQL Actions 검증 완료

## 진행 중 항목

- 없음

## 다음 작업

- GitHub 저장소 공개 전환과 repository topics 설정 여부 결정

## 미결정 사항

- GitHub 저장소 공개 전환 시점과 repository topics

## 확인된 제약과 위험

- 현재 개발 환경은 Apple Silicon arm64이다. Microsoft SQL Server Linux 컨테이너는 Intel·AMD x86-64 Linux 호스트만 공식 지원하며 에뮬레이션·변환 환경은 테스트하거나 지원하지 않는다.
- Apple Silicon 로컬 에뮬레이션은 공식 검증에서 제외하며, 완료된 MSSQL 검증 결과는 GitHub Actions x86-64 Linux 실행만 근거로 한다.
- Spring Boot 4.1.1 BOM의 Flyway 12.4.0은 H2 2.4.240을 최신 검증 범위보다 높은 버전으로 경고한다. 현재 V1~V5 마이그레이션과 전체 테스트는 통과했으며, 확인된 호환성 문제가 없으므로 개별 버전은 고정하지 않는다.
- H2 2.4.240 기반 테스트 조합에서 이동 유형 `CHECK`를 `IN` 또는 동등한 `OR`로 정의했을 때 INSERT 중 `Check constraint invalid`와 `database has been closed` 예외가 관찰됐다. 직접 원인은 확정하지 않았다. 동일 허용값의 `CASE` 제약은 MSSQL V1~V5 적용과 정상 이동 흐름에서도 통과했다.

## 검증 결과

- PR #1을 merge commit `af12ac1`로 병합해 단계별 문제 해결 커밋 이력을 원격 기본 브랜치 `main`에 보존했다.
- Apple Silicon arm64 환경에서 Temurin JDK 21 설치와 `java`, `javac`, macOS JDK 탐지를 검증했다.
- Docker 29.5.3과 Docker Compose v5.1.4가 설치되어 있다.
- Gradle Wrapper 8.14.3이 Temurin JDK 21에서 실행됨을 확인했다.
- Stage 2에서 `./gradlew clean test`를 실행해 테스트 27건이 모두 통과했다.
- Stage 2에서 `./gradlew check`가 성공했다.
- H2 빈 스키마에 Flyway V1이 적용되고 Hibernate `ddl-auto=validate`가 통과함을 확인했다.
- `Instant` 값의 `TIMESTAMP` 저장·재조회 결과가 일치함을 확인했다.
- `sku-001`과 `SKU-001`의 동시 등록에서 정확히 한 요청만 성공하고 정규화된 상품 한 건만 저장됨을 확인했다.
- local 프로필 애플리케이션을 임시 포트 18080으로 기동해 상품 등록 `201`, 단건·목록 조회 `200`, 검증 실패 `400`, 상품 없음 `404`, SKU 중복 `409`와 각 `errorCode`를 확인했다.
- MockMvc로 깨진 JSON 본문이 `400 MALFORMED_REQUEST`로 반환되고 쿼리 파라미터 타입 오류는 `400 VALIDATION_FAILED`를 유지함을 확인했다.
- 변경 후 health `200/UP`, OpenAPI JSON `200`과 상품 API 경로 생성을 확인한 뒤 애플리케이션을 종료했다.
- V2/V3 마이그레이션과 Hibernate `ddl-auto=validate`가 통과했다.
- V1 적용 후 생성된 기존 상품에 V2가 0 재고를 backfill하는 과정을 별도 H2 DB에서 확인했다.
- 신규 상품과 0 재고의 원자성, 입고와 이력의 원자성 및 실패 rollback을 확인했다.
- 동일 상품에 두 입고를 병렬 실행해 최종 현재재고와 `RECEIPT` 이력 개수가 일치함을 H2에서 확인했다. 이 결과는 MSSQL 잠금 동작의 증거로 사용하지 않는다.
- 재고 unique/check 제약과 상품·유형 복합 필터 및 고정 정렬을 확인했다.
- Stage 3 반영 후 `./gradlew clean test`와 `./gradlew check`를 실행해 테스트 51건이 실패·건너뜀 없이 통과했다.
- local 프로필을 임시 포트 18080으로 기동해 신규 상품의 0 재고 조회 `200`, 입고 `200`, `RECEIPT` 이력 조회 `200`, 수량 검증 실패 `400`, 상품 없음 `404`를 확인했다.
- 같은 실행에서 health, OpenAPI JSON, Swagger UI가 모두 HTTP `200`을 반환함을 확인한 뒤 애플리케이션을 종료했다.
- Stage 4 반영 후 `./gradlew clean test`를 실행해 테스트 76건이 모두 통과했다.
- 최종 상태에서 `./gradlew clean check`를 실행해 테스트 76건이 실패·건너뜀 없이 통과했다.
- local 프로필을 임시 포트 18080으로 기동해 복수 상품 주문 생성 `201`, 주문 조회 `200`, 예약 재고·`RESERVATION` 이력·`orderId` 필터 결과를 확인했다.
- 같은 실행에서 재고 부족 `409 INSUFFICIENT_STOCK`, 중복 주문 항목 `400 DUPLICATE_ORDER_ITEM`, health·OpenAPI·Swagger UI `200`을 확인한 뒤 애플리케이션을 종료했다.
- Stage 5 반영 후 첫 `./gradlew clean test`에서 98건 중 기존 rollback 테스트 1건이 실패했다. 같은 테스트 클래스의 이전 메서드가 저장한 주문이 남아 전역 주문 0건 assertion을 깨뜨린 것이 원인이었고, 서비스 rollback 결함은 아니었다.
- rollback 테스트 클래스에 FK 순서의 테스트 데이터 전·후처리를 추가한 뒤 `./gradlew clean test`를 다시 실행해 98건이 모두 통과했다.
- 최종 상태에서 `./gradlew clean check`를 실행해 테스트 98건이 실패·건너뜀 없이 통과했다.
- local 프로필을 임시 포트 18080으로 기동해 출고 완료와 주문 취소가 각각 `200`을 반환하고 주문 상태, 처리 시각, 재고 수치, `SHIPMENT`·`RESERVATION_RELEASE` 이력이 일치함을 확인했다.
- 출고·취소된 주문의 동일·교차 재처리 네 경우가 모두 `409 INVALID_ORDER_STATE`를 반환함을 확인했다.
- 같은 실행에서 health, OpenAPI JSON, Swagger UI가 모두 HTTP `200`을 반환하고 OpenAPI에 출고·취소 경로가 생성됨을 확인한 뒤 애플리케이션을 종료했다.
- Stage 6 반영 후 `./gradlew clean test`를 실행해 테스트 101건이 실패·건너뜀 없이 통과했다.
- 최종 상태에서 `./gradlew clean check`를 실행해 테스트 101건이 실패·건너뜀 없이 통과했다.
- DTO·메서드 파라미터·타입 변환·SKU 도메인 검증이 모두 `400 VALIDATION_FAILED`와 구조화된 필드 오류를 반환함을 확인했다.
- 깨진 JSON은 `400 MALFORMED_REQUEST`, 리소스 없음은 404, 도메인 충돌은 409로 기존 계약을 유지함을 확인했다.
- 테스트 DB에서 예약재고 불일치를 만든 뒤 출고 요청이 내부 메시지를 노출하지 않는 `500 INTERNAL_SERVER_ERROR`를 반환하고 주문·재고 변경이 롤백됨을 확인했다.
- 예상하지 못한 DB 예외의 내부 메시지와 예외 클래스가 응답에 노출되지 않으며, 미등록 경로 404와 지원하지 않는 메서드 405가 포괄 500 처리에 가려지지 않음을 확인했다.
- OpenAPI에 API별 400·404·409·500 응답과 `ApiProblemDetail.errorCode`, `fieldErrors`가 생성되고 내부 확장 맵은 스키마에서 제외됨을 자동화 테스트로 확인했다.
- local 프로필을 임시 포트 18080으로 기동해 health `200`, SKU 도메인 검증 `400 VALIDATION_FAILED`와 `fieldErrors`, API별 오류 응답, 내부 확장 맵이 제외된 OpenAPI 스키마를 확인한 뒤 애플리케이션을 종료했다.
- Stage 7 신규 동시성 통합 테스트 5건을 Gradle 작업 재실행 옵션으로 포함해 총 3회 실행했고 모두 통과했다.
- 동일 상품의 입고·예약 경쟁에서 두 변경과 이력이 모두 보존되고, 합계가 가용재고와 같은 두 예약은 모두 성공함을 H2에서 확인했다.
- 가용재고를 초과하는 두 예약에서는 기존 테스트와 같이 정확히 하나만 성공하고 초과 예약이 발생하지 않음을 확인했다.
- 동일 주문의 동시 출고에서는 정확히 한 요청만 성공하고, 출고·취소 경쟁에서는 둘 중 하나만 최종 상태와 해당 이력을 생성함을 확인했다.
- 상품 입력 순서가 반대인 복수 상품 주문 두 건이 timeout 없이 끝나고 최종 예약재고와 `RESERVATION` 이력이 일치함을 확인했다.
- Stage 7 반영 후 `./gradlew clean test`를 실행해 테스트 106건이 실패·건너뜀 없이 통과했다.
- 최종 상태에서 `./gradlew clean check`를 실행해 테스트 106건이 실패·건너뜀 없이 통과했다.
- 이번 H2 검증에서 DB 잠금 대기, 커넥션 풀 고갈, 테스트 동기화 실패로 인한 timeout은 관찰되지 않았다. 이 결과는 MSSQL 잠금 동작의 증거로 사용하지 않는다.
- Stage 8 시작 시 Docker 29.5.3, Docker Compose v5.1.4와 Linux/aarch64 데몬의 정상 실행을 확인했다.
- Temurin 21 JDK·JRE Jammy 이미지가 linux/amd64와 linux/arm64를 지원함을 레지스트리 매니페스트로 확인했다.
- `docker compose config`가 기본 호스트 포트 18080과 전용 네트워크 구성을 정상 해석했다.
- `docker compose build`로 `logiops:local` 멀티스테이지 이미지를 실제 빌드했다.
- 이미지 아키텍처는 arm64, 크기는 152,547,113바이트이며 Temurin 21.0.12.1 JRE로 실행됨을 확인했다.
- 컨테이너가 UID 999 비루트 사용자로 실행되고 `/app`에는 `app.jar`만 있으며 빌드 작업공간·소스·Gradle 실행 파일이 없음을 확인했다.
- Compose 컨테이너가 `healthy` 상태가 되고 health·OpenAPI·Swagger UI가 모두 HTTP `200`을 반환함을 확인했다.
- 컨테이너에서 상품 등록 `201`과 해당 상품의 0 재고 조회 `200`을 확인했다.
- 검증 후 `docker compose down`으로 LogiOps 컨테이너와 전용 네트워크만 제거했으며 `logiops:local` 이미지는 유지했다.
- 지정된 문서 11개의 존재 여부와 Markdown 내부 링크 대상을 확인했다.
- 이전 기준인 Spring Boot 3, `com.logiops`, springdoc-openapi 2.x가 현재 선택값으로 잘못 남아 있지 않은지 검사했다. 관련 문자열은 변경 이유를 설명하는 `DECISIONS.md`에만 존재한다.
- Markdown 줄 끝 불필요 공백을 검사했으며 발견되지 않았다.
- MSSQL 의존성 추가와 DB별 migration 분리 후 `./gradlew clean check --no-daemon`을 실행해 기존 H2 테스트 106건이 실패·오류·건너뜀 없이 통과했다.
- MSSQL 전용 테스트 소스가 기존 테스트와 함께 컴파일되고 일반 `test`에서는 `mssql` tag가 제외됨을 확인했다.
- `dependencyInsight`로 Spring Boot BOM이 `mssql-jdbc:13.4.0.jre11`, `flyway-sqlserver:12.4.0`을 선택함을 확인했다.
- `./gradlew mssqlTest --dry-run --no-daemon`으로 전용 task 구성을 확인했다. 실제 MSSQL 접속과 테스트는 실행하지 않았다.
- Ruby YAML parser로 워크플로 파일의 기본 YAML 구문을 확인했다. 로컬에는 `actionlint`가 설치되어 있지 않아 GitHub Actions 전용 정적 검사는 실행하지 못했으며, 워크플로의 실제 유효성은 기능 브랜치 push 후 GitHub Actions에서 확인해야 한다.
- GitHub 저장소를 `origin`으로 등록하고 `feature/stage-9-mssql` 브랜치를 push했다. 원격 `main`의 초기 커밋을 조상으로 갖는 선형 이력임을 확인했다.
- 첫 MSSQL Actions 실행은 SQL Server 관리자 비밀번호가 8자 미만이어서 준비 단계에서 실패했다. 비밀번호 값은 로그에서 마스킹됐고 정책에 맞게 Actions secret을 수정한 뒤 재실행했다.
- 두 번째 MSSQL Actions 실행에서는 SQL Server 2022 x64 컨테이너 기동과 `logiops` DB 생성이 성공했다.
- 같은 실행의 기존 H2 테스트 106건 중 저장 전후 `Instant` 완전 일치를 검사하는 5건이 Linux 환경에서 실패해 `mssqlTest`는 실행되지 않았다.
- 실패 지점과 공통 패턴을 근거로 시스템 Clock과 DB 컬럼의 정밀도 차이를 원인으로 판단하고 공통 Clock을 마이크로초 단위로 고정했다. 실제 DB·드라이버의 반올림 또는 절삭 방식은 전제로 두지 않는다.
- 시간 정밀도 수정 후 `./gradlew clean check --no-daemon`을 실행해 H2 테스트 107건이 실패·오류·건너뜀 없이 통과했다.
- 다음 Actions 실행에서 H2 테스트 107건은 통과했지만 MSSQL V5가 새 컬럼을 같은 batch에서 참조해 `Invalid column name 'outbound_order_id'`로 실패했다. DDL 사이를 `GO`로 분리해 SQL Server batch 컴파일 문제를 해결했다.
- V5 수정 후 Flyway V1~V5 적용은 성공했고, Hibernate validation이 `Instant`에 `DATETIMEOFFSET(7)`을 기대하지만 migration은 `DATETIME2(6)`인 불일치를 검출했다. MSSQL V1~V4 시간 컬럼을 dialect 기대 타입으로 정정했다.
- GitHub Actions x86-64 SQL Server 2022 실행 [36706136444](https://github.com/krapnuyij/logiops/actions/runs/36706136444)에서 H2 `test`와 MSSQL `mssqlTest` task가 모두 성공했다.
- MSSQL에서 빈 DB Flyway V1~V5, Hibernate `ddl-auto=validate`, IDENTITY·시간·Unicode 매핑, 핵심 출고·취소 흐름과 UNIQUE·CHECK·FK 거부 동작을 확인했다.
- MSSQL에서 초과 동시 예약, 동일 주문 출고·취소 경쟁, 역순 다중 상품 주문이 timeout 없이 끝나고 최종 주문·재고·이력 정합성을 유지함을 확인했다. 이 결과는 H2 검증 결과와 구분해 기록한다.
- Stage 10에서 `./gradlew clean check --no-daemon`을 실행해 H2 테스트 107건이 실패·오류·건너뜀 없이 통과했다.
- `.git`, `.gradle`, `build`를 제외한 임시 작업 디렉터리에서 같은 Gradle 검증을 다시 실행해 성공했다.
- 임시 작업 디렉터리의 local 프로필을 18081 포트로 기동해 health·OpenAPI·Swagger UI `200`, 상품 등록 `201`, 입고·주문 생성·출고·취소·재고·이력 조회 `200`을 확인했다. 예제 흐름의 최종 재고는 현재 16, 예약 0, 가용 16이었다.
- 같은 임시 작업 디렉터리에서 `docker compose config`와 `docker compose up --build --detach`를 실행했다. 컨테이너가 `healthy`가 되고 health·OpenAPI·Swagger UI `200`, 상품 등록 `201`, 입고·재고 조회 `200`을 반환함을 확인했다.
- Docker 컨테이너가 UID 999로 실행되고 `/app`에 `app.jar`만 포함함을 확인한 뒤, 검증용 LogiOps 컨테이너와 전용 네트워크만 제거했다.
- Markdown 내부 링크, 이전 단계 상태 표현, 줄 끝 공백과 민감정보 형식 후보를 검사했다. 발견된 민감정보는 없으며 추적 중인 환경 파일은 가짜 값만 사용하는 `.env.example`뿐이다.
- 변경한 GitHub Actions YAML을 macOS 기본 Ruby YAML parser로 읽어 기본 구문을 확인했다.
- main 병합 push로 시작된 GitHub Actions 실행 [36712963102](https://github.com/krapnuyij/logiops/actions/runs/36712963102)에서 SQL Server 준비, H2 `test`, MSSQL `mssqlTest`와 컨테이너 정리가 모두 성공해 main trigger의 실제 동작을 확인했다.
