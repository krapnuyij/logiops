package io.github.krapnuyij.logiops.order;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundOrderMigrationTest {

  private static final String JDBC_URL = "jdbc:h2:mem:order-migration-test;DB_CLOSE_DELAY=-1";

  @AfterEach
  void dropDatabase() throws Exception {
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      statement.execute("DROP ALL OBJECTS");
    }
  }

  @Test
  void keepsExistingReceiptWithoutOrderReferenceWhenApplyingV4AndV5() throws Exception {
    Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration/h2")
        .target("3")
        .load()
        .migrate();

    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      statement.executeUpdate("""
          INSERT INTO products (sku, name, created_at)
          VALUES ('SKU-LEGACY', '기존 상품', TIMESTAMP '2026-09-30 01:00:00')
          """);
      statement.executeUpdate("""
          INSERT INTO inventories (product_id, on_hand_quantity, reserved_quantity, updated_at)
          SELECT id, 10, 0, TIMESTAMP '2026-09-30 01:10:00'
          FROM products WHERE sku = 'SKU-LEGACY'
          """);
      statement.executeUpdate("""
          INSERT INTO stock_movements (
            product_id, movement_type, on_hand_delta, reserved_delta,
            on_hand_after, reserved_after, occurred_at
          )
          SELECT id, 'RECEIPT', 10, 0, 10, 0, TIMESTAMP '2026-09-30 01:10:00'
          FROM products WHERE sku = 'SKU-LEGACY'
          """);
    }

    Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration/h2")
        .load()
        .migrate();

    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery("""
          SELECT movement_type, outbound_order_id
          FROM stock_movements
          """);

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getString("movement_type")).isEqualTo("RECEIPT");
      assertThat(resultSet.getObject("outbound_order_id")).isNull();
      assertThat(resultSet.next()).isFalse();

      assertThatThrownBy(() -> statement.executeUpdate("""
          INSERT INTO stock_movements (
            product_id, outbound_order_id, movement_type,
            on_hand_delta, reserved_delta, on_hand_after, reserved_after, occurred_at
          )
          SELECT id, NULL, 'RESERVATION', 0, 1, 10, 1, TIMESTAMP '2026-09-30 01:20:00'
          FROM products WHERE sku = 'SKU-LEGACY'
          """))
          .isInstanceOf(SQLException.class);
    }
  }

  @Test
  void enforcesOrderStatusTimestampsAndAllowsFinalStockMovements() throws Exception {
    Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration/h2")
        .load()
        .migrate();

    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      statement.executeUpdate("""
          INSERT INTO products (sku, name, created_at)
          VALUES ('SKU-FINAL', '처리 상품', TIMESTAMP '2026-09-30 01:00:00')
          """);
      statement.executeUpdate("""
          INSERT INTO outbound_orders (status, created_at, shipped_at, cancelled_at)
          VALUES (
            'SHIPPED',
            TIMESTAMP '2026-09-30 01:10:00',
            TIMESTAMP '2026-09-30 01:20:00',
            NULL
          )
          """);
      statement.executeUpdate("""
          INSERT INTO outbound_orders (status, created_at, shipped_at, cancelled_at)
          VALUES (
            'CANCELLED',
            TIMESTAMP '2026-09-30 01:30:00',
            NULL,
            TIMESTAMP '2026-09-30 01:40:00'
          )
          """);
      statement.executeUpdate("""
          INSERT INTO stock_movements (
            product_id, outbound_order_id, movement_type,
            on_hand_delta, reserved_delta, on_hand_after, reserved_after, occurred_at
          )
          SELECT product.id, outbound_order.id, 'SHIPMENT',
            -1, -1, 9, 0, TIMESTAMP '2026-09-30 01:20:00'
          FROM products product
          CROSS JOIN outbound_orders outbound_order
          WHERE product.sku = 'SKU-FINAL' AND outbound_order.status = 'SHIPPED'
          """);
      statement.executeUpdate("""
          INSERT INTO stock_movements (
            product_id, outbound_order_id, movement_type,
            on_hand_delta, reserved_delta, on_hand_after, reserved_after, occurred_at
          )
          SELECT product.id, outbound_order.id, 'RESERVATION_RELEASE',
            0, -1, 10, 0, TIMESTAMP '2026-09-30 01:40:00'
          FROM products product
          CROSS JOIN outbound_orders outbound_order
          WHERE product.sku = 'SKU-FINAL' AND outbound_order.status = 'CANCELLED'
          """);

      assertThatThrownBy(() -> statement.executeUpdate("""
          INSERT INTO outbound_orders (status, created_at, shipped_at, cancelled_at)
          VALUES (
            'SHIPPED',
            TIMESTAMP '2026-09-30 02:00:00',
            NULL,
            NULL
          )
          """))
          .isInstanceOf(SQLException.class);

      ResultSet resultSet = statement.executeQuery("""
          SELECT COUNT(*) AS movement_count
          FROM stock_movements
          WHERE movement_type IN ('SHIPMENT', 'RESERVATION_RELEASE')
          """);
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getLong("movement_count")).isEqualTo(2);
    }
  }

  private Connection connection() throws Exception {
    return DriverManager.getConnection(JDBC_URL, "sa", "");
  }
}
