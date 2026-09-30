# LogiOps

LogiOps는 단일 물류 거점의 상품, 재고, 출고 주문, 재고 이동 이력을 관리하는 Spring Boot 기반 미니 물류 서비스이다. 개인 포트폴리오 프로젝트로서 기능 수보다 트랜잭션, 재고 정합성, 동시성 제어, 테스트 근거를 명확하게 보여주는 데 초점을 둔다.

## 현재 상태

Stage 9까지 완료했다. H2와 MSSQL Flyway 스크립트, MSSQL 프로필, x86-64 GitHub Actions 워크플로를 분리했으며 기존 H2 테스트와 MSSQL 전용 테스트를 각각 실행한다.

GitHub Actions x86-64 SQL Server 2022에서 Flyway V1~V5, JPA validation, 핵심 업무 흐름, DB 제약과 비관적 잠금 동시성 테스트가 통과했다. 이 과정에서 H2로 드러나지 않았던 V5 DDL batch 경계와 `Instant`의 `DATETIMEOFFSET(7)` 매핑을 실제 MSSQL 결과에 맞게 수정했다. Apple Silicon 로컬 에뮬레이션은 공식 검증 결과로 사용하지 않는다.

## 구현된 업무 흐름

1. 상품 등록과 조회
2. 상품 입고
3. 현재·예약·가용재고 조회
4. 출고 주문 생성과 재고 예약
5. 출고 완료 또는 주문 취소
6. 재고 이동 이력 조회

가용재고는 `현재재고 - 예약재고`로 계산한다. 주문 상태와 재고 변경은 하나의 트랜잭션에서 처리하며 H2와 MSSQL의 동시성 결과를 구분해 검증했다.

## 확정된 기술 기준

- Java 21
- Spring Boot 4.1.1
- Gradle Wrapper 8.14.3, Groovy DSL
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- H2, Microsoft SQL Server 2022, Flyway
- springdoc-openapi-starter-webmvc-ui 3.1.1
- Actuator
- JUnit 5, AssertJ, Mockito, MockMvc
- Docker, Docker Compose
- GitHub Actions x86-64 MSSQL 검증

루트 Java 패키지는 `io.github.krapnuyij.logiops`이다.

## 문서

- [프로젝트 명세](PROJECT_SPEC.md)
- [현재 단계](CURRENT_STAGE.md)
- [아키텍처](docs/ARCHITECTURE.md)
- [도메인 모델](docs/DOMAIN_MODEL.md)
- [API 명세 초안](docs/API_SPEC.md)
- [테스트 전략](docs/TEST_STRATEGY.md)
- [개발 로드맵](docs/ROADMAP.md)
- [주요 의사결정](docs/DECISIONS.md)
- [에이전트 작업 규칙](AGENTS.md)

## 실행 방법

사전 요구사항은 JDK 21이다. 별도의 시스템 Gradle 설치 없이 저장소의 Gradle Wrapper를 사용한다.

테스트를 실행한다.

```bash
./gradlew test
```

local 프로필로 애플리케이션을 실행한다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

기본 포트는 `8080`이다. 실행 후 다음 경로를 확인할 수 있다.

- Health: <http://localhost:8080/actuator/health>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>

local 프로필은 인메모리 H2를 사용하며 애플리케이션을 종료하면 데이터가 유지되지 않는다. 시작 시 Flyway가 상품·재고·주문·재고 이동 이력 마이그레이션을 적용하고 Hibernate가 Entity와 스키마를 검증한다.

## Docker 실행 방법

Docker 이미지 빌드와 컨테이너 기동을 함께 수행한다.

```bash
docker compose up --build --detach
docker compose ps
```

Compose의 기본 호스트 포트는 `18080`이고 컨테이너 내부 포트는 `8080`이다.

- Health: <http://localhost:18080/actuator/health>
- OpenAPI JSON: <http://localhost:18080/v3/api-docs>
- Swagger UI: <http://localhost:18080/swagger-ui/index.html>

호스트 포트를 바꾸려면 실행 시 `LOGIOPS_PORT`를 지정한다.

```bash
LOGIOPS_PORT=19090 docker compose up --build --detach
```

검증을 마치면 이번 프로젝트의 컨테이너와 네트워크를 종료한다.

```bash
docker compose down
```

Docker 실행도 local 프로필의 인메모리 H2를 사용하므로 컨테이너를 제거하면 데이터가 사라진다. 실행 이미지는 비루트 사용자로 동작하며 health 상태는 `/actuator/health`로 확인한다.

## MSSQL 검증 방법

MSSQL 검증은 Microsoft가 지원하는 x86-64 Linux 환경인 GitHub Actions `ubuntu-24.04`에서 SQL Server 2022 서비스 컨테이너를 사용한다. Apple Silicon의 로컬 에뮬레이션 결과는 공식 검증으로 사용하지 않는다.

기능 브랜치의 GitHub Actions 실행 [36706136444](https://github.com/krapnuyij/logiops/actions/runs/36706136444)에서 빈 DB migration, JPA validation, 핵심 통합·DB 제약·동시성 테스트가 통과했다. H2 검증은 빠른 회귀 테스트의 근거이고 이 x86-64 실행은 MSSQL 호환성의 근거이다.

저장소의 Actions secret에 강한 임시 비밀번호를 `MSSQL_SA_PASSWORD` 이름으로 등록한다. 워크플로는 main 이외의 기능 브랜치 push 또는 기본 브랜치의 수동 실행으로 시작되며, 빈 `logiops` DB를 만들고 H2 테스트와 `mssqlTest`를 순서대로 실행한다. 비밀번호는 저장소 파일이나 로그에 기록하지 않는다.

외부에서 준비한 MSSQL을 직접 사용할 때는 다음 환경변수를 설정한다. `.env.example`은 키와 형식만 보여주는 가짜 값이며 애플리케이션이 자동으로 읽지 않는다.

```bash
export MSSQL_URL='jdbc:sqlserver://HOST:1433;databaseName=logiops;encrypt=true;trustServerCertificate=true'
export MSSQL_USERNAME='sa'
export MSSQL_PASSWORD='직접 설정한 비밀번호'
./gradlew mssqlTest
```

`mssqlTest`는 기존 `test`와 분리되어 있으므로 일반 로컬 테스트는 MSSQL 접속을 요구하지 않는다.

상품을 등록한다.

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -d '{"sku":"sku-001","name":"테스트 상품"}' \
  http://localhost:8080/api/v1/products
```

상품을 단건 또는 페이지 단위로 조회한다.

```bash
curl http://localhost:8080/api/v1/products/1
curl 'http://localhost:8080/api/v1/products?page=0&size=20'
```

등록된 상품의 0 재고를 확인하고 입고한다.

```bash
curl http://localhost:8080/api/v1/inventories/1
curl -X POST \
  -H 'Content-Type: application/json' \
  -d '{"quantity":10}' \
  http://localhost:8080/api/v1/inventories/1/receipts
```

상품과 이동 유형으로 재고 이동 이력을 조회한다.

```bash
curl 'http://localhost:8080/api/v1/stock-movements?productId=1&type=RECEIPT&page=0&size=20'
```

입고한 상품으로 출고 주문을 생성하고 예약 이력을 조회한다.

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -d '{"items":[{"productId":1,"quantity":4}]}' \
  http://localhost:8080/api/v1/outbound-orders
curl http://localhost:8080/api/v1/outbound-orders/1
curl 'http://localhost:8080/api/v1/stock-movements?orderId=1&type=RESERVATION'
```

예약된 주문을 출고 완료하거나 취소한다. 두 작업은 모두 요청 본문을 사용하지 않는다.

```bash
curl -X POST http://localhost:8080/api/v1/outbound-orders/1/ship
curl -X POST http://localhost:8080/api/v1/outbound-orders/2/cancel
curl 'http://localhost:8080/api/v1/stock-movements?orderId=1&type=SHIPMENT'
curl 'http://localhost:8080/api/v1/stock-movements?orderId=2&type=RESERVATION_RELEASE'
```

상품·재고·주문 API의 상세 계약은 [API 명세](docs/API_SPEC.md)를 참고한다.

## 개발 원칙

- 한 번에 승인된 단계만 구현한다.
- 핵심 도메인 변경에는 테스트를 함께 작성한다.
- H2 결과로 MSSQL 호환성을 단정하지 않는다.
- 구현하거나 검증하지 않은 기능을 완료된 것으로 문서화하지 않는다.
- commit, push, PR은 사용자 요청이 있을 때만 수행한다.
