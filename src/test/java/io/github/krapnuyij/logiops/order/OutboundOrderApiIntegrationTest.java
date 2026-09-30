package io.github.krapnuyij.logiops.order;

import java.util.List;

import io.github.krapnuyij.logiops.inventory.InventoryRepository;
import io.github.krapnuyij.logiops.inventory.InventoryService;
import io.github.krapnuyij.logiops.inventory.StockMovementType;
import io.github.krapnuyij.logiops.inventory.StockMovementRepository;
import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OutboundOrderApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @Autowired
  private OutboundOrderRepository outboundOrderRepository;

  @Autowired
  private OutboundOrderService outboundOrderService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearData() {
    deleteAllData();
  }

  @AfterEach
  void cleanUpData() {
    deleteAllData();
  }

  @Test
  void createsAndGetsReservedOrderWithItemsSortedByProductId() throws Exception {
    Product firstProduct = productWithStock("SKU-001", 10);
    Product secondProduct = productWithStock("SKU-002", 8);

    MvcResult result = mockMvc.perform(post("/api/v1/outbound-orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderJson(secondProduct.getId(), 2, firstProduct.getId(), 4)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern(
            "/api/v1/outbound-orders/[0-9]+"
        )))
        .andExpect(jsonPath("$.status").value("RESERVED"))
        .andExpect(jsonPath("$.items[0].productId").value(firstProduct.getId()))
        .andExpect(jsonPath("$.items[0].sku").value("SKU-001"))
        .andExpect(jsonPath("$.items[0].quantity").value(4))
        .andExpect(jsonPath("$.items[1].productId").value(secondProduct.getId()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.shippedAt").value(nullValue()))
        .andExpect(jsonPath("$.cancelledAt").value(nullValue()))
        .andReturn();

    String location = result.getResponse().getHeader("Location");
    mockMvc.perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RESERVED"))
        .andExpect(jsonPath("$.items[0].productId").value(firstProduct.getId()))
        .andExpect(jsonPath("$.items[1].productId").value(secondProduct.getId()));
  }

  @Test
  void rejectsEmptyItemsAndInvalidQuantity() throws Exception {
    mockMvc.perform(post("/api/v1/outbound-orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"items\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

    mockMvc.perform(post("/api/v1/outbound-orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"items":[{"productId":1,"quantity":0}]}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].quantity"))
        .andExpect(jsonPath("$.fieldErrors[0].reason").value(
            "주문 수량은 양수여야 한다."
        ));

    mockMvc.perform(post("/api/v1/outbound-orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"items\":[null]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void rejectsDuplicateProduct() throws Exception {
    Product product = productWithStock("SKU-001", 10);

    mockMvc.perform(post("/api/v1/outbound-orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderJson(product.getId(), 1, product.getId(), 2)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("DUPLICATE_ORDER_ITEM"));
  }

  @Test
  void returnsConflictWhenStockIsInsufficient() throws Exception {
    Product product = productWithStock("SKU-001", 3);

    mockMvc.perform(post("/api/v1/outbound-orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(singleItemOrderJson(product.getId(), 4)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
  }

  @Test
  void returnsNotFoundForUnknownProductAndOrder() throws Exception {
    mockMvc.perform(post("/api/v1/outbound-orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(singleItemOrderJson(999, 1)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));

    mockMvc.perform(get("/api/v1/outbound-orders/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
  }

  @Test
  void shipsReservedOrder() throws Exception {
    Product product = productWithStock("SHIP-API", 10);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ));

    mockMvc.perform(post("/api/v1/outbound-orders/{orderId}/ship", order.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(order.getId()))
        .andExpect(jsonPath("$.status").value("SHIPPED"))
        .andExpect(jsonPath("$.shippedAt").exists())
        .andExpect(jsonPath("$.cancelledAt").value(nullValue()));

    var inventory = inventoryService.getByProductId(product.getId());
    assertThat(inventory.getOnHandQuantity()).isEqualTo(6);
    assertThat(inventory.getReservedQuantity()).isZero();
    assertThat(stockMovementRepository.findAll()).anySatisfy(movement -> {
      assertThat(movement.getOutboundOrderId()).isEqualTo(order.getId());
      assertThat(movement.getType()).isEqualTo(StockMovementType.SHIPMENT);
    });
  }

  @Test
  void cancelsReservedOrder() throws Exception {
    Product product = productWithStock("CANCEL-API", 10);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ));

    mockMvc.perform(post("/api/v1/outbound-orders/{orderId}/cancel", order.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(order.getId()))
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.shippedAt").value(nullValue()))
        .andExpect(jsonPath("$.cancelledAt").exists());

    var inventory = inventoryService.getByProductId(product.getId());
    assertThat(inventory.getOnHandQuantity()).isEqualTo(10);
    assertThat(inventory.getReservedQuantity()).isZero();
    assertThat(stockMovementRepository.findAll()).anySatisfy(movement -> {
      assertThat(movement.getOutboundOrderId()).isEqualTo(order.getId());
      assertThat(movement.getType()).isEqualTo(StockMovementType.RESERVATION_RELEASE);
    });
  }

  @Test
  void returnsConflictWhenFinalOrderIsProcessedAgain() throws Exception {
    Product product = productWithStock("FINAL-API", 10);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ));
    outboundOrderService.ship(order.getId());

    mockMvc.perform(post("/api/v1/outbound-orders/{orderId}/ship", order.getId()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ORDER_STATE"));
    mockMvc.perform(post("/api/v1/outbound-orders/{orderId}/cancel", order.getId()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ORDER_STATE"));
  }

  @Test
  void returnsNotFoundWhenFinalizingUnknownOrder() throws Exception {
    mockMvc.perform(post("/api/v1/outbound-orders/999/ship"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
    mockMvc.perform(post("/api/v1/outbound-orders/999/cancel"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
  }

  @Test
  void returnsSafeInternalServerErrorForReservationMismatch() throws Exception {
    Product product = productWithStock("MISMATCH-API", 10);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ));
    jdbcTemplate.update(
        "update inventories set reserved_quantity = 0 where product_id = ?",
        product.getId()
    );

    MvcResult result = mockMvc.perform(post(
            "/api/v1/outbound-orders/{orderId}/ship",
            order.getId()
        ))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.type").value("about:blank"))
        .andExpect(jsonPath("$.title").value("Internal Server Error"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.detail").value("예상하지 못한 서버 오류가 발생했다."))
        .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.fieldErrors").doesNotExist())
        .andReturn();

    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("requested", "reserved", "InventoryReservationMismatchException");
    OutboundOrder unchanged = outboundOrderRepository.findDetailedById(order.getId())
        .orElseThrow();
    assertThat(unchanged.getStatus()).isEqualTo(OutboundOrderStatus.RESERVED);
    assertThat(inventoryService.getByProductId(product.getId()).getOnHandQuantity()).isEqualTo(10);
    assertThat(inventoryService.getByProductId(product.getId()).getReservedQuantity()).isZero();
  }

  private Product productWithStock(String sku, long quantity) {
    Product product = productService.create(sku, "상품");
    inventoryService.receive(product.getId(), quantity);
    return product;
  }

  private String singleItemOrderJson(long productId, long quantity) {
    return """
        {"items":[{"productId":%d,"quantity":%d}]}
        """.formatted(productId, quantity);
  }

  private String orderJson(
      long firstProductId,
      long firstQuantity,
      long secondProductId,
      long secondQuantity
  ) {
    return """
        {
          "items": [
            {"productId": %d, "quantity": %d},
            {"productId": %d, "quantity": %d}
          ]
        }
        """.formatted(firstProductId, firstQuantity, secondProductId, secondQuantity);
  }

  private void deleteAllData() {
    stockMovementRepository.deleteAll();
    outboundOrderRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }
}
