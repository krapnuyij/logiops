package io.github.krapnuyij.logiops.product;

import java.net.URI;

import io.github.krapnuyij.logiops.common.error.ApiProblemDetail;
import io.github.krapnuyij.logiops.product.dto.CreateProductRequest;
import io.github.krapnuyij.logiops.product.dto.ProductPageResponse;
import io.github.krapnuyij.logiops.product.dto.ProductResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @ApiResponses({
      @ApiResponse(
          responseCode = "400",
          description = "요청 검증 실패 또는 JSON 본문 오류",
          content = @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ApiProblemDetail.class)
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "SKU 중복",
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
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
    Product product = productService.create(request.sku(), request.name());
    URI location = URI.create("/api/v1/products/" + product.getId());
    return ResponseEntity.created(location).body(ProductResponse.from(product));
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
          description = "상품 없음",
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
  public ProductResponse getById(@PathVariable @Positive long productId) {
    return ProductResponse.from(productService.getById(productId));
  }

  @ApiResponses({
      @ApiResponse(
          responseCode = "400",
          description = "페이지 요청 검증 실패",
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
  public ProductPageResponse getAll(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return ProductPageResponse.from(productService.getAll(page, size));
  }
}
