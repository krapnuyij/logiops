package io.github.krapnuyij.logiops.inventory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryMigrationTest {

  private static final String JDBC_URL = "jdbc:h2:mem:inventory-migration-test;DB_CLOSE_DELAY=-1";

  @AfterEach
  void dropDatabase() throws Exception {
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      statement.execute("DROP ALL OBJECTS");
    }
  }

  @Test
  void backfillsZeroInventoryForProductCreatedBeforeV2() throws Exception {
    Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration/h2")
        .target("1")
        .load()
        .migrate();

    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      statement.executeUpdate("""
          INSERT INTO products (sku, name, created_at)
          VALUES ('SKU-LEGACY', '기존 상품', TIMESTAMP '2026-09-30 01:00:00')
          """);
    }

    Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration/h2")
        .load()
        .migrate();

    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery("""
          SELECT i.on_hand_quantity, i.reserved_quantity, i.updated_at, p.created_at
          FROM inventories i
          JOIN products p ON p.id = i.product_id
          WHERE p.sku = 'SKU-LEGACY'
          """);

      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getLong("on_hand_quantity")).isZero();
      assertThat(resultSet.getLong("reserved_quantity")).isZero();
      assertThat(resultSet.getTimestamp("updated_at"))
          .isEqualTo(resultSet.getTimestamp("created_at"));
      assertThat(resultSet.next()).isFalse();
    }
  }

  private Connection connection() throws Exception {
    return DriverManager.getConnection(JDBC_URL, "sa", "");
  }
}
