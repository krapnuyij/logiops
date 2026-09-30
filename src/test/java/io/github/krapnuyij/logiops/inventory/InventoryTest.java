package io.github.krapnuyij.logiops.inventory;

import java.time.Instant;

import io.github.krapnuyij.logiops.product.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryTest {

  private static final Instant CREATED_AT = Instant.parse("2026-09-30T01:00:00Z");
  private static final Instant RECEIVED_AT = Instant.parse("2026-09-30T02:00:00Z");

  @Test
  void initializesWithZeroQuantities() {
    Inventory inventory = Inventory.initialize(product(), CREATED_AT);

    assertThat(inventory.getOnHandQuantity()).isZero();
    assertThat(inventory.getReservedQuantity()).isZero();
    assertThat(inventory.getAvailableQuantity()).isZero();
    assertThat(inventory.getUpdatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void receivesQuantityAndUpdatesAvailableQuantityAndTime() {
    Inventory inventory = Inventory.initialize(product(), CREATED_AT);

    inventory.receive(10, RECEIVED_AT);

    assertThat(inventory.getOnHandQuantity()).isEqualTo(10);
    assertThat(inventory.getReservedQuantity()).isZero();
    assertThat(inventory.getAvailableQuantity()).isEqualTo(10);
    assertThat(inventory.getUpdatedAt()).isEqualTo(RECEIVED_AT);
  }

  @Test
  void rejectsNonPositiveReceiptQuantity() {
    Inventory inventory = Inventory.initialize(product(), CREATED_AT);

    assertThatThrownBy(() -> inventory.receive(0, RECEIVED_AT))
        .isInstanceOf(InvalidInventoryException.class)
        .extracting("errorCode")
        .isEqualTo("VALIDATION_FAILED");
    assertThatThrownBy(() -> inventory.receive(-1, RECEIVED_AT))
        .isInstanceOf(InvalidInventoryException.class);
  }

  @Test
  void rejectsQuantityOverflowWithoutChangingState() {
    Inventory inventory = Inventory.initialize(product(), CREATED_AT);
    inventory.receive(Long.MAX_VALUE, RECEIVED_AT);

    assertThatThrownBy(() -> inventory.receive(1, RECEIVED_AT.plusSeconds(1)))
        .isInstanceOf(InvalidInventoryException.class)
        .hasMessageContaining("허용 범위");
    assertThat(inventory.getOnHandQuantity()).isEqualTo(Long.MAX_VALUE);
    assertThat(inventory.getUpdatedAt()).isEqualTo(RECEIVED_AT);
  }

  @Test
  void reservesAvailableQuantity() {
    Inventory inventory = Inventory.initialize(product(), CREATED_AT);
    inventory.receive(10, RECEIVED_AT);

    inventory.reserve(4, RECEIVED_AT.plusSeconds(1));

    assertThat(inventory.getOnHandQuantity()).isEqualTo(10);
    assertThat(inventory.getReservedQuantity()).isEqualTo(4);
    assertThat(inventory.getAvailableQuantity()).isEqualTo(6);
    assertThat(inventory.getUpdatedAt()).isEqualTo(RECEIVED_AT.plusSeconds(1));
  }

  @Test
  void rejectsReservationGreaterThanAvailableQuantity() {
    Inventory inventory = Inventory.initialize(product(), CREATED_AT);
    inventory.receive(10, RECEIVED_AT);

    assertThatThrownBy(() -> inventory.reserve(11, RECEIVED_AT.plusSeconds(1)))
        .isInstanceOf(InsufficientStockException.class)
        .extracting("errorCode")
        .isEqualTo("INSUFFICIENT_STOCK");
    assertThat(inventory.getReservedQuantity()).isZero();
    assertThat(inventory.getAvailableQuantity()).isEqualTo(10);
  }

  @Test
  void rejectsNonPositiveReservationQuantity() {
    Inventory inventory = Inventory.initialize(product(), CREATED_AT);

    assertThatThrownBy(() -> inventory.reserve(0, RECEIVED_AT))
        .isInstanceOf(InvalidInventoryException.class)
        .extracting("errorCode")
        .isEqualTo("VALIDATION_FAILED");
  }

  @Test
  void shipsReservedQuantity() {
    Inventory inventory = inventoryWithReservation(10, 4);
    Instant shippedAt = RECEIVED_AT.plusSeconds(2);

    inventory.ship(4, shippedAt);

    assertThat(inventory.getOnHandQuantity()).isEqualTo(6);
    assertThat(inventory.getReservedQuantity()).isZero();
    assertThat(inventory.getAvailableQuantity()).isEqualTo(6);
    assertThat(inventory.getUpdatedAt()).isEqualTo(shippedAt);
  }

  @Test
  void releasesReservedQuantity() {
    Inventory inventory = inventoryWithReservation(10, 4);
    Instant cancelledAt = RECEIVED_AT.plusSeconds(2);

    inventory.release(4, cancelledAt);

    assertThat(inventory.getOnHandQuantity()).isEqualTo(10);
    assertThat(inventory.getReservedQuantity()).isZero();
    assertThat(inventory.getAvailableQuantity()).isEqualTo(10);
    assertThat(inventory.getUpdatedAt()).isEqualTo(cancelledAt);
  }

  @Test
  void rejectsShipmentAndReleaseGreaterThanReservedQuantityWithoutChangingState() {
    Inventory inventory = inventoryWithReservation(10, 4);

    assertThatThrownBy(() -> inventory.ship(5, RECEIVED_AT.plusSeconds(2)))
        .isInstanceOf(InventoryReservationMismatchException.class);
    assertThatThrownBy(() -> inventory.release(5, RECEIVED_AT.plusSeconds(2)))
        .isInstanceOf(InventoryReservationMismatchException.class);

    assertThat(inventory.getOnHandQuantity()).isEqualTo(10);
    assertThat(inventory.getReservedQuantity()).isEqualTo(4);
    assertThat(inventory.getUpdatedAt()).isEqualTo(RECEIVED_AT.plusSeconds(1));
  }

  @Test
  void rejectsNonPositiveShipmentAndReleaseQuantity() {
    Inventory inventory = inventoryWithReservation(10, 4);

    assertThatThrownBy(() -> inventory.ship(0, RECEIVED_AT.plusSeconds(2)))
        .isInstanceOf(InvalidInventoryException.class);
    assertThatThrownBy(() -> inventory.release(0, RECEIVED_AT.plusSeconds(2)))
        .isInstanceOf(InvalidInventoryException.class);
  }

  private Inventory inventoryWithReservation(long onHandQuantity, long reservedQuantity) {
    Inventory inventory = Inventory.initialize(product(), CREATED_AT);
    inventory.receive(onHandQuantity, RECEIVED_AT);
    inventory.reserve(reservedQuantity, RECEIVED_AT.plusSeconds(1));
    return inventory;
  }

  private Product product() {
    return Product.create("SKU-001", "상품", CREATED_AT);
  }
}
