# API 명세

## 1. 상태

이 문서는 구현된 REST API의 요청·응답과 오류 계약을 정의한다. 상품 API는 Stage 2, 입고·재고 API는 Stage 3, 주문 생성·조회와 예약 이동 이력 API는 Stage 4, 출고 완료·주문 취소 API는 Stage 5에서 구현했다. Stage 6에서 공통 오류 응답과 API별 OpenAPI 계약을 구현했으며 관련 API 테스트와 실제 HTTP 호출로 검증했다.

## 2. 공통 규칙

- Base path: `/api/v1`
- Content-Type: `application/json`
- 시간 형식: UTC 기반 ISO 8601 문자열
- 식별자: 양의 정수 `Long`
- 수량: 양의 정수 `long`
- 목록 기본 페이지: `page=0`, `size=20`
- `page`는 0 이상, `size`는 1~100이어야 하며 범위를 벗어나면 `400 VALIDATION_FAILED`를 반환한다.
- 성공 응답은 별도 공통 envelope 없이 용도별 DTO를 반환한다.

## 3. 상품 API

### 3.1 상품 등록

`POST /api/v1/products`

요청 예시:

```json
{
  "sku": "SKU-001",
  "name": "테스트 상품"
}
```

입력 규칙:

- `sku`: `String.strip()` 후 `^[A-Za-z0-9._-]{1,64}$`를 만족해야 한다.
- `sku`: `Locale.ROOT` 기준 대문자로 정규화하며 정규화된 값을 저장하고 응답한다.
- `name`: `String.strip()` 후 1~100자이며 별도 문자셋 whitelist를 두지 않는다.

예를 들어 `sku-001`을 요청하면 저장값과 응답값은 `SKU-001`이다.

응답:

- `201 Created`
- `Location: /api/v1/products/{id}`

```json
{
  "id": 1,
  "sku": "SKU-001",
  "name": "테스트 상품",
  "createdAt": "2026-09-30T01:00:00Z"
}
```

오류:

- 필수값·길이·문자셋 검증 실패: `400 VALIDATION_FAILED`
- SKU 중복: `409 DUPLICATE_SKU`

### 3.2 상품 단건 조회

`GET /api/v1/products/{productId}`

- 성공: `200 OK`
- 상품 없음: `404 PRODUCT_NOT_FOUND`

### 3.3 상품 목록 조회

`GET /api/v1/products?page=0&size=20`

별도 정렬 입력은 받지 않으며 상품 ID 오름차순으로 반환한다.

응답 예시:

```json
{
  "content": [
    {
      "id": 1,
      "sku": "SKU-001",
      "name": "테스트 상품",
      "createdAt": "2026-09-30T01:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

## 4. 재고 API

### 4.1 입고

`POST /api/v1/inventories/{productId}/receipts`

요청 예시:

```json
{
  "quantity": 10
}
```

응답: `200 OK`

```json
{
  "productId": 1,
  "onHandQuantity": 10,
  "reservedQuantity": 0,
  "availableQuantity": 10,
  "updatedAt": "2026-09-30T01:10:00Z"
}
```

오류:

- 수량이 양수가 아님: `400 VALIDATION_FAILED`
- 상품 없음: `404 PRODUCT_NOT_FOUND`

### 4.2 재고 조회

`GET /api/v1/inventories/{productId}`

- 성공: `200 OK`
- 상품 없음: `404 PRODUCT_NOT_FOUND`
- 재고 행 없음: `404 INVENTORY_NOT_FOUND`

Stage 3 마이그레이션 이후 등록된 모든 상품은 재고 행을 가져야 하므로 `INVENTORY_NOT_FOUND`는 데이터 무결성 문제를 식별하는 용도로 남긴다.

## 5. 출고 주문 API

### 5.1 주문 생성 및 재고 예약

`POST /api/v1/outbound-orders`

요청 예시:

```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 4
    },
    {
      "productId": 2,
      "quantity": 2
    }
  ]
}
```

응답:

- `201 Created`
- `Location: /api/v1/outbound-orders/{id}`

```json
{
  "id": 100,
  "status": "RESERVED",
  "items": [
    {
      "productId": 1,
      "sku": "SKU-001",
      "quantity": 4
    },
    {
      "productId": 2,
      "sku": "SKU-002",
      "quantity": 2
    }
  ],
  "createdAt": "2026-09-30T01:20:00Z",
  "shippedAt": null,
  "cancelledAt": null
}
```

오류:

- 주문 항목 없음 또는 수량 오류: `400 VALIDATION_FAILED`
- 같은 상품 중복: `400 DUPLICATE_ORDER_ITEM`
- 상품 없음: `404 PRODUCT_NOT_FOUND`
- 재고 행 없음: `404 INVENTORY_NOT_FOUND`
- 가용재고 부족: `409 INSUFFICIENT_STOCK`

복수 항목 중 하나라도 실패하면 주문과 모든 예약을 생성하지 않는다.
요청 항목 순서에는 업무 의미를 두지 않으며 응답 항목은 상품 ID 오름차순으로 반환한다.

### 5.2 주문 조회

`GET /api/v1/outbound-orders/{orderId}`

- 성공: `200 OK`
- 주문 없음: `404 ORDER_NOT_FOUND`

### 5.3 출고 완료

`POST /api/v1/outbound-orders/{orderId}/ship`

요청 본문은 없다.

- 성공: `200 OK`, 변경된 주문 응답
- 주문 없음: `404 ORDER_NOT_FOUND`
- 이미 출고 또는 취소됨: `409 INVALID_ORDER_STATE`

주문 상태, 상품별 현재·예약재고, `SHIPMENT` 이력을 하나의 트랜잭션에서 변경한다.

### 5.4 주문 취소

`POST /api/v1/outbound-orders/{orderId}/cancel`

요청 본문은 없다.

- 성공: `200 OK`, 변경된 주문 응답
- 주문 없음: `404 ORDER_NOT_FOUND`
- 이미 출고 또는 취소됨: `409 INVALID_ORDER_STATE`

주문 상태, 상품별 예약재고, `RESERVATION_RELEASE` 이력을 하나의 트랜잭션에서 변경한다.

## 6. 재고 이동 이력 API

### 6.1 목록 조회

`GET /api/v1/stock-movements`

선택 query parameter:

| 이름 | 형식 | 설명 |
|---|---|---|
| `productId` | `Long` | 상품 필터 |
| `orderId` | `Long` | 출고 주문 필터 |
| `type` | enum | 이동 유형 필터 |
| `page` | integer | 0부터 시작 |
| `size` | integer | 기본 20, 최대 100 |

정렬은 `occurredAt DESC, id DESC`로 고정한다.

응답 항목 예시:

```json
{
  "id": 1000,
  "productId": 1,
  "orderId": null,
  "type": "RECEIPT",
  "onHandDelta": 10,
  "reservedDelta": 0,
  "onHandAfter": 10,
  "reservedAfter": 0,
  "availableAfter": 10,
  "occurredAt": "2026-09-30T01:10:00Z"
}
```

`productId`, `orderId`, `type`, `page`, `size` 조건을 지원한다. 입고 이력의 `orderId`는 `null`이고 예약 이력에는 생성 원인인 주문 ID가 포함된다. 존재하지 않는 필터 ID는 오류가 아니라 빈 페이지를 반환한다.

## 7. 운영 엔드포인트

| Method | 경로 | 목적 |
|---|---|---|
| `GET` | `/actuator/health` | 애플리케이션 상태 확인 |
| `GET` | `/v3/api-docs` | OpenAPI JSON |
| `GET` | `/swagger-ui/index.html` | Swagger UI |

위 세 경로는 local 프로필과 Docker 실행환경에서 실제 응답을 확인했다.

## 8. 오류 응답

Spring `ProblemDetail`을 기반으로 한 `ApiProblemDetail`을 사용한다. 모든 명시적 API 오류는 `application/problem+json`으로 응답하고 `errorCode`를 포함한다.

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "상품 SKU-001의 가용재고가 부족합니다.",
  "instance": "/api/v1/outbound-orders",
  "errorCode": "INSUFFICIENT_STOCK"
}
```

필드 검증 실패 시 `fieldErrors` 확장 필드를 추가한다.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "요청 값이 올바르지 않습니다.",
  "instance": "/api/v1/products",
  "errorCode": "VALIDATION_FAILED",
  "fieldErrors": [
    {
      "field": "sku",
      "reason": "공백일 수 없습니다."
    }
  ]
}
```

`fieldErrors`의 각 항목은 `field`와 안전한 `reason`으로 구성한다. 구조화할 필드가 없는 오류에서는 `fieldErrors`를 응답에서 생략한다. DTO Bean Validation, 메서드 파라미터 제약, 요청 파라미터 타입 변환, 필드가 지정된 도메인 검증은 `400 VALIDATION_FAILED`로 통일한다. 깨진 JSON이나 JSON 본문 타입 오류는 `400 MALFORMED_REQUEST`로 구분한다.

예상하지 못한 오류는 서버 로그에 원본 예외를 기록하되 응답에는 고정된 `500 INTERNAL_SERVER_ERROR` 코드와 안전한 설명만 제공한다. 출고·취소 시 저장된 주문 항목 수량보다 예약재고가 적은 경우는 클라이언트 입력 오류가 아니라 Aggregate 간 정합성 오류로 보고 이 응답을 사용한다. 내부 예외 메시지, 예외 클래스, SQL, 스택 추적은 응답에 포함하지 않는다.

## 9. 오류 코드

| HTTP | 코드 | 의미 |
|---:|---|---|
| 400 | `VALIDATION_FAILED` | 요청 필드 검증 실패 |
| 400 | `MALFORMED_REQUEST` | JSON 형식 또는 타입 오류 |
| 400 | `DUPLICATE_ORDER_ITEM` | 주문 내 상품 중복 |
| 404 | `PRODUCT_NOT_FOUND` | 상품 없음 |
| 404 | `INVENTORY_NOT_FOUND` | 재고 행 없음 |
| 404 | `ORDER_NOT_FOUND` | 주문 없음 |
| 409 | `DUPLICATE_SKU` | SKU 중복 |
| 409 | `INSUFFICIENT_STOCK` | 가용재고 부족 |
| 409 | `INVALID_ORDER_STATE` | 허용되지 않는 주문 상태 전이 |
| 500 | `INTERNAL_SERVER_ERROR` | 예상하지 못한 서버 오류 |

내부 구현 정보와 원본 DB 오류는 응답에 포함하지 않는다.
