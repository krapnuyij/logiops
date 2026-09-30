package io.github.krapnuyij.logiops.mssql;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.krapnuyij.logiops.inventory.Inventory;
import io.github.krapnuyij.logiops.inventory.InventoryRepository;
import io.github.krapnuyij.logiops.inventory.InventoryService;
import io.github.krapnuyij.logiops.inventory.StockMovementRepository;
import io.github.krapnuyij.logiops.inventory.StockMovementType;
import io.github.krapnuyij.logiops.order.OrderItemCommand;
import io.github.krapnuyij.logiops.order.OutboundOrder;
import io.github.krapnuyij.logiops.order.OutboundOrderRepository;
import io.github.krapnuyij.logiops.order.OutboundOrderService;
import io.github.krapnuyij.logiops.order.OutboundOrderStatus;
import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("mssql")
@Tag("mssql")
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class MssqlCompatibilityIntegrationTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @Autowired
  private OutboundOrderService outboundOrderService;

  @Autowired
  private OutboundOrderRepository outboundOrderRepository;

  @BeforeEach
  void clearData() {
    deleteAllData();
  }

  @AfterEach
  void cleanUpData() {
    deleteAllData();
  }

  @Test
  void startsOnSqlServerWithAllMigrationsAndExpectedColumnTypes() {
    String databaseVersion = jdbcTemplate.queryForObject("SELECT @@VERSION", String.class);
    Integer migrationCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
        Integer.class
    );

    assertThat(databaseVersion).contains("Microsoft SQL Server");
    assertThat(migrationCount).isEqualTo(5);
    assertThat(columnType("products", "sku")).isEqualTo("nvarchar");
    assertThat(columnType("products", "name")).isEqualTo("nvarchar");
    assertThat(columnType("products", "created_at")).isEqualTo("datetime2");
    assertThat(columnType("stock_movements", "movement_type")).isEqualTo("nvarchar");
    assertThat(identityFlag("products", "id")).isEqualTo(1);
  }

  @Test
  void preservesUnicodeAndTimestampsThroughShipmentAndCancellationFlows() {
    Product shippedProduct = productService.create(" mssql-ship ", "  테스트 상품  ");
    Product reloadedProduct = productService.getById(shippedProduct.getId());

    assertThat(reloadedProduct.getSku()).isEqualTo("MSSQL-SHIP");
    assertThat(reloadedProduct.getName()).isEqualTo("테스트 상품");
    assertThat(Duration.between(
        shippedProduct.getCreatedAt(),
        reloadedProduct.getCreatedAt()
    ).abs()).isLessThanOrEqualTo(Duration.ofNanos(1_000));

    inventoryService.receive(shippedProduct.getId(), 10);
    OutboundOrder shippedOrder = outboundOrderService.create(List.of(
        new OrderItemCommand(shippedProduct.getId(), 4)
    ));
    outboundOrderService.ship(shippedOrder.getId());

    Product cancelledProduct = productService.create("mssql-cancel", "취소 검증 상품");
    inventoryService.receive(cancelledProduct.getId(), 8);
    OutboundOrder cancelledOrder = outboundOrderService.create(List.of(
        new OrderItemCommand(cancelledProduct.getId(), 3)
    ));
    outboundOrderService.cancel(cancelledOrder.getId());

    assertThat(outboundOrderService.getById(shippedOrder.getId()).getStatus())
        .isEqualTo(OutboundOrderStatus.SHIPPED);
    assertThat(outboundOrderService.getById(cancelledOrder.getId()).getStatus())
        .isEqualTo(OutboundOrderStatus.CANCELLED);
    assertInventory(shippedProduct.getId(), 6, 0, 6);
    assertInventory(cancelledProduct.getId(), 8, 0, 8);
    assertThat(movementCount(shippedOrder.getId(), StockMovementType.RESERVATION)).isEqualTo(1);
    assertThat(movementCount(shippedOrder.getId(), StockMovementType.SHIPMENT)).isEqualTo(1);
    assertThat(movementCount(
        cancelledOrder.getId(),
        StockMovementType.RESERVATION_RELEASE
    )).isEqualTo(1);
  }

  @Test
  void enforcesUniqueCheckAndForeignKeyConstraints() {
    Product product = productService.create("MSSQL-CONSTRAINT", "제약 검증 상품");
    inventoryService.receive(product.getId(), 5);

    assertThatThrownBy(() -> jdbcTemplate.update(
        "INSERT INTO products (sku, name, created_at) VALUES (?, ?, SYSUTCDATETIME())",
        product.getSku(),
        "중복 상품"
    )).isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(() -> jdbcTemplate.update(
        "UPDATE inventories SET reserved_quantity = on_hand_quantity + 1 WHERE product_id = ?",
        product.getId()
    )).isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(() -> jdbcTemplate.update(
        """
        INSERT INTO inventories (
          product_id, on_hand_quantity, reserved_quantity, updated_at
        ) VALUES (?, 0, 0, SYSUTCDATETIME())
        """,
        Long.MAX_VALUE
    )).isInstanceOf(DataIntegrityViolationException.class);
  }

  private String columnType(String tableName, String columnName) {
    return jdbcTemplate.queryForObject(
        """
        SELECT DATA_TYPE
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ? AND COLUMN_NAME = ?
        """,
        String.class,
        tableName,
        columnName
    );
  }

  private Integer identityFlag(String tableName, String columnName) {
    return jdbcTemplate.queryForObject(
        "SELECT COLUMNPROPERTY(OBJECT_ID(?), ?, 'IsIdentity')",
        Integer.class,
        tableName,
        columnName
    );
  }

  private void assertInventory(
      long productId,
      long onHandQuantity,
      long reservedQuantity,
      long availableQuantity
  ) {
    Inventory inventory = inventoryService.getByProductId(productId);
    assertThat(inventory.getOnHandQuantity()).isEqualTo(onHandQuantity);
    assertThat(inventory.getReservedQuantity()).isEqualTo(reservedQuantity);
    assertThat(inventory.getAvailableQuantity()).isEqualTo(availableQuantity);
  }

  private long movementCount(long orderId, StockMovementType type) {
    return stockMovementRepository.findAll().stream()
        .filter(movement -> movement.getOutboundOrderId() != null)
        .filter(movement -> movement.getOutboundOrderId() == orderId)
        .filter(movement -> movement.getType() == type)
        .count();
  }

  private void deleteAllData() {
    stockMovementRepository.deleteAll();
    outboundOrderRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }
}
