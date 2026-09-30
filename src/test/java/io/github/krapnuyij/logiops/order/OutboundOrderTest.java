package io.github.krapnuyij.logiops.order;

import java.time.Instant;
import java.util.List;

import io.github.krapnuyij.logiops.product.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundOrderTest {

  private static final Instant CREATED_AT = Instant.parse("2026-09-30T01:00:00Z");

  @Test
  void createsReservedOrderWithItems() {
    Product firstProduct = product("SKU-001");
    Product secondProduct = product("SKU-002");

    OutboundOrder order = OutboundOrder.createReserved(
        List.of(
            new OutboundOrder.ItemDraft(firstProduct, 4),
            new OutboundOrder.ItemDraft(secondProduct, 2)
        ),
        CREATED_AT
    );

    assertThat(order.getStatus()).isEqualTo(OutboundOrderStatus.RESERVED);
    assertThat(order.getItems()).extracting(OutboundOrderItem::getQuantity)
        .containsExactly(4L, 2L);
    assertThat(order.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(order.getShippedAt()).isNull();
    assertThat(order.getCancelledAt()).isNull();
  }

  @Test
  void rejectsEmptyItems() {
    assertThatThrownBy(() -> OutboundOrder.createReserved(List.of(), CREATED_AT))
        .isInstanceOf(InvalidOutboundOrderException.class)
        .extracting("errorCode")
        .isEqualTo("VALIDATION_FAILED");
  }

  @Test
  void rejectsDuplicateProduct() {
    Product product = product("SKU-001");

    assertThatThrownBy(() -> OutboundOrder.createReserved(
        List.of(
            new OutboundOrder.ItemDraft(product, 1),
            new OutboundOrder.ItemDraft(product, 2)
        ),
        CREATED_AT
    )).isInstanceOf(DuplicateOrderItemException.class)
        .extracting("errorCode")
        .isEqualTo("DUPLICATE_ORDER_ITEM");
  }

  @Test
  void rejectsNonPositiveItemQuantity() {
    assertThatThrownBy(() -> OutboundOrder.createReserved(
        List.of(new OutboundOrder.ItemDraft(product("SKU-001"), 0)),
        CREATED_AT
    )).isInstanceOf(InvalidOutboundOrderException.class);
  }

  @Test
  void shipsReservedOrder() {
    OutboundOrder order = order();
    Instant shippedAt = CREATED_AT.plusSeconds(60);

    order.ship(shippedAt);

    assertThat(order.getStatus()).isEqualTo(OutboundOrderStatus.SHIPPED);
    assertThat(order.getShippedAt()).isEqualTo(shippedAt);
    assertThat(order.getCancelledAt()).isNull();
  }

  @Test
  void cancelsReservedOrder() {
    OutboundOrder order = order();
    Instant cancelledAt = CREATED_AT.plusSeconds(60);

    order.cancel(cancelledAt);

    assertThat(order.getStatus()).isEqualTo(OutboundOrderStatus.CANCELLED);
    assertThat(order.getShippedAt()).isNull();
    assertThat(order.getCancelledAt()).isEqualTo(cancelledAt);
  }

  @Test
  void rejectsAnyFurtherTransitionAfterShipment() {
    OutboundOrder order = order();
    order.ship(CREATED_AT.plusSeconds(60));

    assertThatThrownBy(() -> order.ship(CREATED_AT.plusSeconds(120)))
        .isInstanceOf(InvalidOrderStateException.class)
        .extracting("errorCode")
        .isEqualTo("INVALID_ORDER_STATE");
    assertThatThrownBy(() -> order.cancel(CREATED_AT.plusSeconds(120)))
        .isInstanceOf(InvalidOrderStateException.class);
  }

  @Test
  void rejectsAnyFurtherTransitionAfterCancellation() {
    OutboundOrder order = order();
    order.cancel(CREATED_AT.plusSeconds(60));

    assertThatThrownBy(() -> order.ship(CREATED_AT.plusSeconds(120)))
        .isInstanceOf(InvalidOrderStateException.class);
    assertThatThrownBy(() -> order.cancel(CREATED_AT.plusSeconds(120)))
        .isInstanceOf(InvalidOrderStateException.class);
  }

  private OutboundOrder order() {
    return OutboundOrder.createReserved(
        List.of(new OutboundOrder.ItemDraft(product("SKU-001"), 4)),
        CREATED_AT
    );
  }

  private Product product(String sku) {
    return Product.create(sku, "상품", CREATED_AT);
  }
}
