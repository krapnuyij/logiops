package io.github.krapnuyij.logiops.inventory;

import java.time.Instant;

import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryApiIntegrationTest {

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
  void clearData() {
    stockMovementRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }

  @Test
  void getsZeroInventoryAndReceivesQuantity() throws Exception {
    Product product = productService.create("SKU-001", "상품");

    mockMvc.perform(get("/api/v1/inventories/{productId}", product.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(product.getId()))
        .andExpect(jsonPath("$.onHandQuantity").value(0))
        .andExpect(jsonPath("$.reservedQuantity").value(0))
        .andExpect(jsonPath("$.availableQuantity").value(0));

    mockMvc.perform(post("/api/v1/inventories/{productId}/receipts", product.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"quantity\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.onHandQuantity").value(10))
        .andExpect(jsonPath("$.reservedQuantity").value(0))
        .andExpect(jsonPath("$.availableQuantity").value(10))
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void rejectsMissingAndNonPositiveReceiptQuantity() throws Exception {
    Product product = productService.create("SKU-001", "상품");

    mockMvc.perform(post("/api/v1/inventories/{productId}/receipts", product.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    mockMvc.perform(post("/api/v1/inventories/{productId}/receipts", product.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"quantity\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void returnsProductNotFoundForUnknownProduct() throws Exception {
    mockMvc.perform(get("/api/v1/inventories/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));

    mockMvc.perform(post("/api/v1/inventories/999/receipts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"quantity\":1}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
  }

  @Test
  void returnsInventoryNotFoundWhenProductHasNoInventoryRow() throws Exception {
    Product product = productRepository.saveAndFlush(
        Product.create("SKU-ORPHAN", "상품", Instant.parse("2026-09-30T01:00:00Z"))
    );

    mockMvc.perform(get("/api/v1/inventories/{productId}", product.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("INVENTORY_NOT_FOUND"));
  }
}
