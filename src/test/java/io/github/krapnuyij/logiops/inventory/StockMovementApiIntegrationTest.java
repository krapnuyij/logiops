package io.github.krapnuyij.logiops.inventory;

import java.util.List;

import io.github.krapnuyij.logiops.order.OrderItemCommand;
import io.github.krapnuyij.logiops.order.OutboundOrder;
import io.github.krapnuyij.logiops.order.OutboundOrderRepository;
import io.github.krapnuyij.logiops.order.OutboundOrderService;
import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockMovementApiIntegrationTest {

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

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private OutboundOrderRepository outboundOrderRepository;

  @Autowired
  private OutboundOrderService outboundOrderService;

  @BeforeEach
  void clearData() {
    deleteAllData();
  }

  @AfterEach
  void cleanUpData() {
    deleteAllData();
  }

  private void deleteAllData() {
    stockMovementRepository.deleteAll();
    outboundOrderRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }

  @Test
  void filtersReceiptMovementsByProductAndType() throws Exception {
    Product firstProduct = productService.create("SKU-001", "첫 상품");
    Product secondProduct = productService.create("SKU-002", "두 번째 상품");
    inventoryService.receive(firstProduct.getId(), 3);
    inventoryService.receive(firstProduct.getId(), 2);
    inventoryService.receive(secondProduct.getId(), 5);

    mockMvc.perform(get("/api/v1/stock-movements")
            .param("productId", firstProduct.getId().toString())
            .param("type", "RECEIPT")
            .param("page", "0")
            .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].productId").value(firstProduct.getId()))
        .andExpect(jsonPath("$.content[0].orderId").value(nullValue()))
        .andExpect(jsonPath("$.content[0].type").value("RECEIPT"))
        .andExpect(jsonPath("$.content[0].onHandAfter").value(5))
        .andExpect(jsonPath("$.content[0].availableAfter").value(5))
        .andExpect(jsonPath("$.content[1].onHandAfter").value(3));
  }

  @Test
  void returnsEmptyPageForMovementTypeWithoutRecords() throws Exception {
    Product product = productService.create("SKU-001", "상품");
    inventoryService.receive(product.getId(), 3);

    mockMvc.perform(get("/api/v1/stock-movements")
            .param("productId", product.getId().toString())
            .param("type", "RESERVATION"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void filtersReservationMovementByOrderId() throws Exception {
    Product product = productService.create("SKU-001", "상품");
    inventoryService.receive(product.getId(), 10);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ));

    mockMvc.perform(get("/api/v1/stock-movements")
            .param("orderId", order.getId().toString())
            .param("type", "RESERVATION"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].orderId").value(order.getId()))
        .andExpect(jsonPath("$.content[0].type").value("RESERVATION"))
        .andExpect(jsonPath("$.content[0].reservedDelta").value(4))
        .andExpect(jsonPath("$.content[0].availableAfter").value(6));
  }

  @Test
  void rejectsInvalidFilterAndPageParameters() throws Exception {
    mockMvc.perform(get("/api/v1/stock-movements").param("type", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    mockMvc.perform(get("/api/v1/stock-movements").param("productId", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    mockMvc.perform(get("/api/v1/stock-movements").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }
}
