package io.github.krapnuyij.logiops.order;

import java.net.URI;

import io.github.krapnuyij.logiops.common.error.ApiProblemDetail;
import io.github.krapnuyij.logiops.order.dto.CreateOutboundOrderRequest;
import io.github.krapnuyij.logiops.order.dto.OutboundOrderResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outbound-orders")
public class OutboundOrderController {

  private final OutboundOrderService outboundOrderService;

  public OutboundOrderController(OutboundOrderService outboundOrderService) {
    this.outboundOrderService = outboundOrderService;
  }

  @ApiResponses({
      @ApiResponse(
          responseCode = "400",
          description = "주문 요청 검증 실패 또는 JSON 본문 오류",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "상품 또는 재고 없음",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "가용재고 부족",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "예상하지 못한 서버 오류",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      )
  })
  @PostMapping
  public ResponseEntity<OutboundOrderResponse> create(
      @Valid @RequestBody CreateOutboundOrderRequest request
  ) {
    OutboundOrder outboundOrder = outboundOrderService.create(request.toCommands());
    URI location = URI.create("/api/v1/outbound-orders/" + outboundOrder.getId());
    return ResponseEntity.created(location).body(OutboundOrderResponse.from(outboundOrder));
  }

  @ApiResponses({
      @ApiResponse(
          responseCode = "400",
          description = "경로 변수 검증 실패",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "출고 주문 없음",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "예상하지 못한 서버 오류",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      )
  })
  @GetMapping("/{orderId}")
  public OutboundOrderResponse getById(@PathVariable @Positive long orderId) {
    return OutboundOrderResponse.from(outboundOrderService.getById(orderId));
  }

  @ApiResponses({
      @ApiResponse(
          responseCode = "400",
          description = "경로 변수 검증 실패",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "출고 주문 없음",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "허용되지 않는 주문 상태 전이",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "예상하지 못한 서버 오류",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      )
  })
  @PostMapping("/{orderId}/ship")
  public OutboundOrderResponse ship(@PathVariable @Positive long orderId) {
    return OutboundOrderResponse.from(outboundOrderService.ship(orderId));
  }

  @ApiResponses({
      @ApiResponse(
          responseCode = "400",
          description = "경로 변수 검증 실패",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "출고 주문 없음",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "허용되지 않는 주문 상태 전이",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "예상하지 못한 서버 오류",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      )
  })
  @PostMapping("/{orderId}/cancel")
  public OutboundOrderResponse cancel(@PathVariable @Positive long orderId) {
    return OutboundOrderResponse.from(outboundOrderService.cancel(orderId));
  }
}
