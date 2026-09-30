package io.github.krapnuyij.logiops.inventory;

import io.github.krapnuyij.logiops.common.error.ApiProblemDetail;
import io.github.krapnuyij.logiops.inventory.dto.StockMovementPageResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock-movements")
public class StockMovementController {

  private final StockMovementService stockMovementService;

  public StockMovementController(StockMovementService stockMovementService) {
    this.stockMovementService = stockMovementService;
  }

  @ApiResponses({
      @ApiResponse(
          responseCode = "400",
          description = "필터 또는 페이지 요청 검증 실패",
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
  @GetMapping
  public StockMovementPageResponse getAll(
      @RequestParam(required = false) @Positive Long productId,
      @RequestParam(required = false, name = "orderId") @Positive Long outboundOrderId,
      @RequestParam(required = false) StockMovementType type,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return StockMovementPageResponse.from(
        stockMovementService.getAll(productId, outboundOrderId, type, page, size)
    );
  }
}
