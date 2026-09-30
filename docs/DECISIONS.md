# 주요 의사결정

## 상태 정의

- `확정`: 사용자가 명시적으로 승인했거나 프로젝트 요구사항으로 고정된 결정
- `제안`: 현재 문서의 권장안이며 문서 승인 시 확정할 결정
- `보류`: 후속 단계에서 실제 검증 후 결정할 항목
- `대체`: 더 이상 사용하지 않는 결정

## 결정 요약

| ID | 결정 | 상태 |
|---|---|---|
| D-001 | Java 21, Spring Boot 4.1.1 | 확정 |
| D-002 | Gradle Wrapper 8.14.3, Groovy DSL | 확정 |
| D-003 | `io.github.krapnuyij.logiops` 패키지 | 확정 |
| D-004 | 모듈형 모놀리스와 기능 중심 패키지 | 확정 |
| D-005 | H2 개발 후 MSSQL 실제 검증 | 확정 |
| D-006 | Stage 2부터 Flyway와 `ddl-auto=validate` | 확정 |
| D-007 | 단일 창고와 정수 수량 | 확정 |
| D-008 | 가용재고를 파생값으로 계산 | 확정 |
| D-009 | 주문 최초 상태를 `RESERVED`로 단순화 | 확정 |
| D-010 | DB 비관적 잠금과 고정 잠금 순서 | 확정 |
| D-011 | `ProblemDetail` 기반 오류 응답 | 확정 |
| D-012 | springdoc-openapi 3.1.1 | 확정 |
| D-013 | Stage 9 MSSQL 결정 시점과 BOM 정책 | 확정 |
| D-014 | SKU 정규화와 유일성 정책 | 확정 |
| D-015 | Apple Silicon MSSQL 컨테이너 제약 | 확정 |
| D-016 | 상품 생성 시 재고 초기화 의존 방향 | 확정 |
| D-017 | 재고 이동 이력의 주문 식별자 참조 | 확정 |
| D-018 | 멀티스테이지 Docker 이미지와 비루트 런타임 | 확정 |
| D-019 | x86-64 CI MSSQL 검증과 DB별 migration 분리 | 확정 |

## D-001 Java 21과 Spring Boot 4.1.1

- 상태: 확정
- 결정: Java 21 LTS와 Spring Boot 4.1.1을 사용한다.
- 이유: 사용자가 최신 안정 Spring Boot 계열을 선택했고 Java 21은 지원 범위에 포함된다.
- 영향: Spring Framework 7과 Jakarta 기반 API를 사용한다. Spring Boot 3 전용 라이브러리를 그대로 사용할 수 없다.
- 근거: [Spring Boot 시스템 요구사항](https://docs.spring.io/spring-boot/system-requirements.html)

## D-002 Gradle Wrapper 8.14.3과 Groovy DSL

- 상태: 확정
- 결정: 시스템 Gradle이 아닌 Wrapper 8.14.3을 사용한다.
- 이유: 빌드 버전을 저장소에서 고정하고 Java 21과 Spring Boot 4.1.1의 지원 범위를 만족한다.
- 영향: 로컬 Gradle 설치는 필요 없지만 Wrapper 실행을 위한 JDK 21은 필요하다.
- 근거: [Gradle 8.14.3 릴리스 노트](https://docs.gradle.org/8.14.3/release-notes.html)

## D-003 Java 패키지

- 상태: 확정
- 결정: group은 `io.github.krapnuyij`, 루트 패키지는 `io.github.krapnuyij.logiops`로 한다.
- 이유: 개인 GitHub 계정에 기반한 역도메인 형식으로 프로젝트 소유 관계와 패키지 고유성을 명확히 한다.
- 영향: 주요 기능 패키지는 `product`, `inventory`, `order`, `common` 아래에 둔다.

## D-004 애플리케이션 구조

- 상태: 확정
- 결정: 단일 Spring Boot 애플리케이션과 DB로 구성한 모듈형 모놀리스를 사용한다.
- 이유: 재고와 주문을 하나의 로컬 트랜잭션으로 처리할 수 있고 포트폴리오 범위에서 설명 가능하다.
- 대안: 마이크로서비스 분리, 엄격한 hexagonal architecture.
- 제외 이유: 분산 트랜잭션과 모듈 수가 증가해 현재 목표보다 복잡도가 커진다.

## D-005 개발 DB와 최종 DB

- 상태: 확정
- 결정: 초기 개발·테스트는 H2, 최종 호환성 검증은 MSSQL을 사용한다.
- 이유: 초기 피드백 속도와 지원 직무 관련 DB 검증을 모두 확보한다.
- 영향: H2에서는 애플리케이션 로직과 기본 동시성을 테스트한다. H2의 잠금 동작을 MSSQL과 같다고 간주하지 않으며 Stage 9에서 실제 MSSQL로 마이그레이션과 동시성 테스트를 다시 실행한다.

## D-006 Flyway 도입 시점

- 상태: 확정
- 결정:
  - Stage 1은 `ddl-auto=none`으로 실행만 검증한다.
  - Stage 2에서 Flyway와 `V1__create_product_table.sql`을 추가한다.
  - Stage 2부터 `ddl-auto=validate`를 사용한다.
  - 테스트 DB에도 Flyway migration을 실행한다.
  - `create-drop`은 기본 설정으로 사용하지 않는다.
- 이유: 첫 Entity부터 DB 스키마 이력을 코드로 관리하고 MSSQL 전환 시 검증 가능한 근거를 남긴다.
- 시간 매핑: Product의 `Instant createdAt`은 Hibernate의 `TIMESTAMP_UTC` 기본 매핑에 맞춰 H2 `TIMESTAMP` 컬럼을 사용하고 `ddl-auto=validate`와 저장·재조회 테스트로 검증한다.
- 근거: [Spring Boot DB 초기화 가이드](https://docs.spring.io/spring-boot/how-to/data-initialization.html)

## D-007 재고 관리 단위

- 상태: 확정
- 결정: MVP는 단일 논리 창고와 정수 수량만 지원한다.
- 이유: 창고·로케이션·단위를 추가하지 않고 재고 예약과 동시성 규칙에 집중한다.
- 영향: Inventory는 Product와 1:1이다. 수량은 Java `long`과 DB `BIGINT`를 사용한다.
- 확장: 복수 창고는 `(warehouse_id, product_id)` 단위 재고 모델로 별도 설계한다.

## D-008 가용재고 저장 방식

- 상태: 확정
- 결정: 가용재고를 DB 컬럼에 저장하지 않고 `현재재고 - 예약재고`로 계산한다.
- 이유: 중복 상태로 인한 불일치를 피한다.
- 영향: DB는 `현재재고 >= 예약재고` 제약으로 가용재고 음수를 방지한다.

## D-009 주문 상태 모델

- 상태: 확정
- 결정: 주문 상태는 `RESERVED`, `SHIPPED`, `CANCELLED`만 사용한다.
- 이유: 주문 생성과 예약이 하나의 트랜잭션이므로 외부에 노출되는 `CREATED` 중간 상태가 없다.
- 영향: 부분 출고와 부분 취소는 지원하지 않는다.

## D-010 동시성 제어

- 상태: 확정
- 결정: Inventory와 처리 대상 OutboundOrder에 DB 비관적 쓰기 잠금을 사용한다. 복수 Inventory는 상품 ID 오름차순으로 잠근다.
- 이유: 재고 확인과 변경을 직렬화하고 복수 행의 교착상태 가능성을 낮춘다.
- 대안: 낙관적 잠금과 재시도.
- 제외 이유: 충돌 처리와 재시도 정책이 추가되어 초기 구현을 설명하기 복잡해진다.
- 검증: Stage 7에서 H2로 애플리케이션 로직과 기본 동시성을 테스트한다. 비관적 잠금과 트랜잭션 동작은 Stage 9에서 실제 MSSQL로 반드시 다시 검증한다.

## D-011 오류 응답

- 상태: 확정
- 결정: Spring `ProblemDetail`에 `errorCode`와 필요 시 `fieldErrors`를 추가한다.
- 의존 방향: `common.error`는 상태 범주별 공통 상위 예외만 정의하고 기능 예외가 이를 확장한다. 공통 핸들러는 기능 패키지를 직접 참조하지 않는다.
- 이유: 표준 HTTP 문제 형식을 활용하면서 클라이언트가 안정적인 애플리케이션 코드를 사용할 수 있다.
- 영향: 검증 오류는 400, 리소스 없음은 404, 재고 부족·중복 SKU·상태 충돌은 409, 예상하지 못한 오류는 500으로 처리한다.

## D-012 springdoc-openapi 버전

- 상태: 확정
- 결정: `springdoc-openapi-starter-webmvc-ui:3.1.1`을 사용한다.
- 이유: 기존 계획의 2.x는 Spring Boot 3용이며 Boot 4에는 3.x가 필요하다.
- 근거: [springdoc-openapi 프로젝트](https://github.com/springdoc/springdoc-openapi)

## D-013 Stage 9 MSSQL 결정 시점과 BOM 정책

- 상태: 확정
- 결정: MSSQL 이미지, 실행 환경과 DB별 Flyway 스크립트는 Stage 9에서 실제 차이를 확인해 결정하며 최종 선택은 D-019에서 관리한다.
- 검증: 실제 MSSQL에서 JPA 매핑, 마이그레이션, 비관적 잠금과 트랜잭션 동작을 검증한다.
- 의존성: JDBC와 Flyway 관련 의존성은 Spring Boot BOM 관리 버전을 우선한다. 확인된 호환성 문제가 있을 때만 개별 버전 고정을 검토한다.
- Stage 2 관찰: BOM이 선택한 Flyway 12.4.0은 H2 2.4.240이 최신 검증 범위보다 높다는 경고를 출력했다. V1 마이그레이션과 전체 테스트가 통과했으므로 버전을 별도 고정하지 않고 후속 단계에서 계속 관찰한다.
- Stage 3 관찰: H2 2.4.240 기반 테스트 조합에서 이동 유형 `CHECK`를 `IN` 또는 동등한 `OR`로 정의했을 때 INSERT 중 `Check constraint invalid`와 `database has been closed` 예외가 발생했다. 직접 원인은 확인하지 못했다. 허용값은 그대로 두고 `CASE` 표현식으로 바꾸어 V2/V3 마이그레이션과 전체 테스트를 통과했으며, MSSQL에서 해당 제약을 다시 검증한다.
- Stage 9 확인: Spring Boot 4.1.1 BOM은 `mssql-jdbc:13.4.0.jre11`과 `flyway-sqlserver:12.4.0`을 선택한다. 두 의존성 모두 build에 버전을 직접 지정하지 않는다.
- 이유: 실제 MSSQL 검증 전에 환경과 vendor별 스크립트를 추측으로 고정하지 않는다.

## D-014 SKU 정규화와 유일성 정책

- 상태: 확정
- 결정: SKU 입력에 `String.strip()`을 적용하고 `^[A-Za-z0-9._-]{1,64}$`를 검증한 뒤 `Locale.ROOT` 기준 대문자로 정규화한다.
- 저장·응답: 입력 원본을 별도로 보관하지 않고 정규화된 값만 저장하고 응답한다.
- 유일성: 정규화된 값으로 애플리케이션 사전 중복 확인을 수행하고 DB unique 제약을 최종 방어선으로 사용한다.
- 경쟁 조건: 사전 확인과 INSERT 사이의 경쟁으로 발생한 unique 제약 위반은 `409 DUPLICATE_SKU`로 변환한다.
- 이유: 애플리케이션 정상 쓰기 경로에서 실행 환경의 locale과 H2·MSSQL collation 차이에 의존하지 않는 단일 SKU 표현을 유지한다.
- 구현: `Product` 정적 팩토리가 정규화와 검증을 수행해 API 이외의 생성 경로에서도 동일한 불변식을 적용한다.

## D-015 Apple Silicon MSSQL 컨테이너 제약

- 상태: 확정
- 확인된 제약: Microsoft는 SQL Server Linux 컨테이너를 Intel·AMD x86-64 Linux 호스트에서만 지원하며 Rosetta 2, Prism, QEMU 같은 에뮬레이션·변환 환경은 테스트하거나 지원하지 않는다.
- 영향: Apple Silicon 로컬 에뮬레이션은 best-effort 개발 환경일 뿐 공식 지원 검증 환경으로 기록하지 않는다.
- 결정: 로컬 에뮬레이션은 사용하지 않고 GitHub Actions x86-64 Linux 환경을 최종 검증 경로로 사용한다. 세부 구성은 D-019에서 관리한다.
- 근거: [Microsoft SQL Server Linux 컨테이너 공식 문서](https://learn.microsoft.com/en-us/sql/linux/quickstart-install-connect-docker)

## D-016 상품 생성 시 재고 초기화 의존 방향

- 상태: 확정
- 결정: `product` 패키지가 `ProductInventoryInitializer` 인터페이스를 소유하고 `inventory` 패키지의 `DefaultProductInventoryInitializer`가 이를 구현한다.
- 이유: 신규 상품과 0 재고를 같은 트랜잭션에서 생성하면서 `product → inventory` 직접 의존과 기능 패키지 간 순환 의존을 피한다.
- 대안: `ProductService`의 `InventoryRepository` 직접 참조, Spring 애플리케이션 이벤트, 별도 오케스트레이션 계층.
- 제외 이유: 직접 참조는 의존 방향을 위반하고, 이벤트는 필수 동기 흐름을 숨기며, 별도 계층은 현재 규모에 비해 설명 비용이 크다.
- 영향: 재고 초기화 실패는 상품 생성 트랜잭션 전체를 롤백한다. `common`은 어느 기능 패키지도 참조하지 않는다.

## D-017 재고 이동 이력의 주문 식별자 참조

- 상태: 확정
- 결정: `StockMovement`는 `OutboundOrder` Entity 연관관계 대신 nullable `outboundOrderId`를 저장하고 DB FK로 참조 무결성을 보장한다.
- 규칙: `RECEIPT`는 주문 ID가 없어야 하고, `RESERVATION`, `SHIPMENT`, `RESERVATION_RELEASE`에는 주문 ID가 반드시 있어야 한다.
- 이유: 주문 예약은 `order → inventory` 의존이 필요하므로 Inventory가 주문 Entity를 참조하면 기능 패키지 간 순환 의존이 생긴다. 다른 Aggregate는 식별자로 참조해 이를 방지한다.
- 대안: `StockMovement`의 `@ManyToOne OutboundOrder`, 별도 movement 패키지 분리.
- 제외 이유: Entity 연관은 순환 의존을 만들고, 별도 패키지 분리는 현재 규모에서 구조 변경 비용이 크다.
- 영향: 주문 객체 탐색은 제공하지 않지만 주문 ID 필터는 직접 처리할 수 있고 DB FK가 잘못된 ID 저장을 차단한다.

## D-018 멀티스테이지 Docker 이미지와 비루트 런타임

- 상태: 확정
- 결정: Temurin Java 21 JDK Jammy 단계에서 Gradle Wrapper로 실행 JAR을 만들고, Temurin Java 21 JRE Jammy 기반 최종 이미지에서 비루트 사용자로 실행한다.
- health: 최종 이미지에 제공되는 `curl`로 `/actuator/health`를 호출하는 Docker `HEALTHCHECK`를 사용한다.
- 이미지 태그: `21-jdk-jammy`, `21-jre-jammy` 이동 태그를 사용해 Java 21 보안 업데이트를 재빌드 시 반영한다. 빌드 로그에는 실제로 해석된 digest가 남는다.
- 대안: 이미지 digest 고정, 단일 JDK 이미지.
- 제외 이유: digest 고정은 보안 패치 갱신을 수동 관리해야 하고, 단일 JDK 이미지는 런타임에 빌드 도구와 캐시를 불필요하게 포함한다.
- 아키텍처: 두 베이스 이미지가 linux/amd64와 linux/arm64를 제공함을 Stage 8에서 확인했다.
- 영향: 같은 태그라도 재빌드 시 기반 이미지 digest가 바뀔 수 있으므로 검증 결과에는 실제 빌드 시점의 Java 버전과 이미지 정보를 기록한다.

## D-019 x86-64 CI MSSQL 검증과 DB별 migration 분리

- 상태: 확정
- 검증 환경: GitHub Actions `ubuntu-24.04` x86-64 runner에서 `mcr.microsoft.com/mssql/server:2022-latest` 서비스 컨테이너를 사용한다.
- migration: H2는 `db/migration/h2`, MSSQL은 `db/migration/mssql`에서 동일한 V1~V5 이력을 관리한다.
- 타입 대응: MSSQL은 `IDENTITY(1,1)`, `DATETIME2(6)`, `NVARCHAR`를 사용하고 MSSQL 프로필에서 nationalized 문자열 매핑을 활성화한다.
- 테스트 분리: `mssql` JUnit tag와 `mssqlTest` Gradle task를 사용해 일반 H2 테스트가 MSSQL 접속에 의존하지 않게 한다.
- 의존성: Microsoft JDBC와 Flyway SQL Server 모듈은 Spring Boot BOM으로 관리하며 Testcontainers는 추가하지 않는다.
- 비밀정보: SQL Server 관리자 비밀번호는 GitHub Actions secret으로만 주입한다.
- 이유: Apple Silicon 에뮬레이션을 공식 검증으로 오인하지 않으면서 실제 vendor 문법, JPA 매핑, DB 제약과 잠금 동작을 반복 가능한 환경에서 검증한다.
- 검증 상태: 구성과 테스트 코드는 구현했지만 GitHub Actions를 실제 실행하기 전이므로 MSSQL 호환성은 아직 검증 완료로 기록하지 않는다.

## 후속 결정 필요 항목

- 운영 배포를 가정할 경우 인증·인가와 Actuator 노출 정책
