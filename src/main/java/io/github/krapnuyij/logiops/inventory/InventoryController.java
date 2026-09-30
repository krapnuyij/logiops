package io.github.krapnuyij.logiops.inventory;

import io.github.krapnuyij.logiops.common.error.ApiProblemDetail;
import io.github.krapnuyij.logiops.inventory.dto.InventoryResponse;
import io.github.krapnuyij.logiops.inventory.dto.ReceiveInventoryRequest;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventories")
public class InventoryController {

  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
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
          description = "상품 또는 재고 없음",
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
  @GetMapping("/{productId}")
  public InventoryResponse getByProductId(@PathVariable @Positive long productId) {
    return InventoryResponse.from(inventoryService.getByProductId(productId));
  }

  @ApiResponses({
      @ApiResponse(
          responseCode = "400",
          description = "경로 변수 또는 요청 본문 검증 실패",
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
          responseCode = "500",
          description = "예상하지 못한 서버 오류",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      )
  })
  @PostMapping("/{productId}/receipts")
  public InventoryResponse receive(
      @PathVariable @Positive long productId,
      @Valid @RequestBody ReceiveInventoryRequest request
  ) {
    return InventoryResponse.from(inventoryService.receive(productId, request.quantity()));
  }
}
