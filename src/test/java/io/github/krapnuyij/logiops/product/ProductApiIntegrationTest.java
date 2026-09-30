package io.github.krapnuyij.logiops.product;

import io.github.krapnuyij.logiops.inventory.InventoryRepository;
import io.github.krapnuyij.logiops.inventory.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @BeforeEach
  void clearProducts() {
    stockMovementRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }

  @Test
  void createsAndGetsProduct() throws Exception {
    MvcResult createResult = mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sku": " sku-001 ",
                  "name": " 테스트 상품 "
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
            "/api/v1/products/[0-9]+"
        )))
        .andExpect(jsonPath("$.sku").value("SKU-001"))
        .andExpect(jsonPath("$.name").value("테스트 상품"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andReturn();

    String location = createResult.getResponse().getHeader("Location");
    mockMvc.perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku").value("SKU-001"));
  }

  @Test
  void listsProductsInIdAscendingOrder() throws Exception {
    productService.create("SKU-002", "첫 상품");
    productService.create("SKU-001", "두 번째 상품");

    mockMvc.perform(get("/api/v1/products").param("page", "0").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].sku").value("SKU-002"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void returnsValidationProblemForMissingDtoField() throws Exception {
    mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sku": "SKU-001"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("about:blank"))
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("요청 값이 올바르지 않다."))
        .andExpect(jsonPath("$.instance").value("/api/v1/products"))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
        .andExpect(jsonPath("$.fieldErrors[0].reason").value("상품명은 필수이다."))
        .andExpect(result -> assertThat(result.getResolvedException())
            .isInstanceOf(MethodArgumentNotValidException.class));
  }

  @Test
  void returnsValidationProblemForDomainValidation() throws Exception {
    mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sku": "INVALID SKU",
                  "name": "상품"
                }
        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("sku"))
        .andExpect(jsonPath("$.fieldErrors[0].reason").value(
            "SKU는 영문자, 숫자, 점, 밑줄, 하이픈으로 구성된 1~64자여야 한다."
        ))
        .andExpect(result -> assertThat(result.getResolvedException())
            .isInstanceOf(InvalidProductException.class));
  }

  @Test
  void returnsValidationProblemForOutOfRangePageParameter() throws Exception {
    mockMvc.perform(get("/api/v1/products").param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("page"))
        .andExpect(result -> assertThat(result.getResolvedException())
            .isInstanceOf(HandlerMethodValidationException.class));
  }

  @Test
  void acceptsPageSizeBoundaries() throws Exception {
    mockMvc.perform(get("/api/v1/products").param("page", "0").param("size", "1"))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/products").param("page", "0").param("size", "100"))
        .andExpect(status().isOk());
  }

  @Test
  void returnsValidationProblemForInvalidPageSize() throws Exception {
    mockMvc.perform(get("/api/v1/products").param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    mockMvc.perform(get("/api/v1/products").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void returnsValidationProblemForTypeMismatch() throws Exception {
    mockMvc.perform(get("/api/v1/products").param("size", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("size"))
        .andExpect(jsonPath("$.fieldErrors[0].reason").value(
            "요청 값의 형식이 올바르지 않다."
        ))
        .andExpect(result -> assertThat(result.getResolvedException())
            .isInstanceOf(MethodArgumentTypeMismatchException.class));
  }

  @Test
  void returnsMalformedRequestProblemForBrokenJson() throws Exception {
    mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sku\":"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.fieldErrors").doesNotExist())
        .andExpect(result -> assertThat(result.getResolvedException())
            .isInstanceOf(HttpMessageNotReadableException.class));
  }

  @Test
  void returnsNotFoundProblem() throws Exception {
    mockMvc.perform(get("/api/v1/products/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
  }

  @Test
  void returnsConflictProblemForDuplicateSku() throws Exception {
    productService.create("sku-001", "첫 상품");

    mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sku": "SKU-001",
                  "name": "두 번째 상품"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("DUPLICATE_SKU"));
  }
}
