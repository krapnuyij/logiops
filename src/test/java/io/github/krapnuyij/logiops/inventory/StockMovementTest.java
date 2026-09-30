package io.github.krapnuyij.logiops.inventory;

import java.time.Instant;

import io.github.krapnuyij.logiops.product.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockMovementTest {

  private static final Instant CREATED_AT = Instant.parse("2026-09-30T01:00:00Z");

  @Test
  void createsReservationMovementWithOrderId() {
    Inventory inventory = inventoryWithReservation(10, 4);

    StockMovement movement = StockMovement.reservation(
        inventory,
        100L,
        4,
        CREATED_AT.plusSeconds(2)
    );

    assertThat(movement.getOutboundOrderId()).isEqualTo(100L);
    assertThat(movement.getType()).isEqualTo(StockMovementType.RESERVATION);
    assertThat(movement.getOnHandDelta()).isZero();
    assertThat(movement.getReservedDelta()).isEqualTo(4);
    assertThat(movement.getOnHandAfter()).isEqualTo(10);
    assertThat(movement.getReservedAfter()).isEqualTo(4);
    assertThat(movement.getAvailableAfter()).isEqualTo(6);
  }

  @Test
  void rejectsReservationMovementWithoutPositiveOrderId() {
    Inventory inventory = inventoryWithReservation(10, 4);

    assertThatThrownBy(() -> StockMovement.reservation(
        inventory,
        0,
        4,
        CREATED_AT.plusSeconds(2)
    )).isInstanceOf(InvalidInventoryException.class);
  }

  @Test
  void createsShipmentMovementAfterInventoryChange() {
    Inventory inventory = inventoryWithReservation(10, 4);
    inventory.ship(4, CREATED_AT.plusSeconds(3));

    StockMovement movement = StockMovement.shipment(
        inventory,
        100L,
        4,
        CREATED_AT.plusSeconds(3)
    );

    assertThat(movement.getType()).isEqualTo(StockMovementType.SHIPMENT);
    assertThat(movement.getOutboundOrderId()).isEqualTo(100L);
    assertThat(movement.getOnHandDelta()).isEqualTo(-4);
    assertThat(movement.getReservedDelta()).isEqualTo(-4);
    assertThat(movement.getOnHandAfter()).isEqualTo(6);
    assertThat(movement.getReservedAfter()).isZero();
    assertThat(movement.getAvailableAfter()).isEqualTo(6);
  }

  @Test
  void createsReservationReleaseMovementAfterInventoryChange() {
    Inventory inventory = inventoryWithReservation(10, 4);
    inventory.release(4, CREATED_AT.plusSeconds(3));

    StockMovement movement = StockMovement.reservationRelease(
        inventory,
        100L,
        4,
        CREATED_AT.plusSeconds(3)
    );

    assertThat(movement.getType()).isEqualTo(StockMovementType.RESERVATION_RELEASE);
    assertThat(movement.getOutboundOrderId()).isEqualTo(100L);
    assertThat(movement.getOnHandDelta()).isZero();
    assertThat(movement.getReservedDelta()).isEqualTo(-4);
    assertThat(movement.getOnHandAfter()).isEqualTo(10);
    assertThat(movement.getReservedAfter()).isZero();
    assertThat(movement.getAvailableAfter()).isEqualTo(10);
  }

  private Inventory inventoryWithReservation(long onHandQuantity, long reservedQuantity) {
    Product product = Product.create("SKU-001", "상품", CREATED_AT);
    Inventory inventory = Inventory.initialize(product, CREATED_AT);
    inventory.receive(onHandQuantity, CREATED_AT.plusSeconds(1));
    inventory.reserve(reservedQuantity, CREATED_AT.plusSeconds(2));
    return inventory;
  }
}
