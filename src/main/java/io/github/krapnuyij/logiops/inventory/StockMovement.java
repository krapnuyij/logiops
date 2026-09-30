package io.github.krapnuyij.logiops.inventory;

import java.time.Instant;
import java.util.Objects;

import io.github.krapnuyij.logiops.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false, updatable = false)
  private Product product;

  @Column(name = "outbound_order_id", updatable = false)
  private Long outboundOrderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type", nullable = false, length = 32, updatable = false)
  private StockMovementType type;

  @Column(name = "on_hand_delta", nullable = false, updatable = false)
  private long onHandDelta;

  @Column(name = "reserved_delta", nullable = false, updatable = false)
  private long reservedDelta;

  @Column(name = "on_hand_after", nullable = false, updatable = false)
  private long onHandAfter;

  @Column(name = "reserved_after", nullable = false, updatable = false)
  private long reservedAfter;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected StockMovement() {
  }

  private StockMovement(
      Product product,
      Long outboundOrderId,
      StockMovementType type,
      long onHandDelta,
      long reservedDelta,
      long onHandAfter,
      long reservedAfter,
      Instant occurredAt
  ) {
    this.product = Objects.requireNonNull(product, "상품은 필수이다.");
    this.outboundOrderId = outboundOrderId;
    this.type = Objects.requireNonNull(type, "재고 이동 유형은 필수이다.");
    this.onHandDelta = onHandDelta;
    this.reservedDelta = reservedDelta;
    this.onHandAfter = onHandAfter;
    this.reservedAfter = reservedAfter;
    this.occurredAt = Objects.requireNonNull(occurredAt, "재고 이동 시각은 필수이다.");
    validateState();
  }

  public static StockMovement receipt(Inventory inventory, long quantity, Instant occurredAt) {
    if (quantity <= 0) {
      throw new InvalidInventoryException("입고 수량은 양수여야 한다.");
    }
    return new StockMovement(
        inventory.getProduct(),
        null,
        StockMovementType.RECEIPT,
        quantity,
        0,
        inventory.getOnHandQuantity(),
        inventory.getReservedQuantity(),
        occurredAt
    );
  }

  public static StockMovement reservation(
      Inventory inventory,
      long outboundOrderId,
      long quantity,
      Instant occurredAt
  ) {
    if (outboundOrderId <= 0) {
      throw new InvalidInventoryException("출고 주문 ID는 양수여야 한다.");
    }
    if (quantity <= 0) {
      throw new InvalidInventoryException("예약 수량은 양수여야 한다.");
    }
    return new StockMovement(
        inventory.getProduct(),
        outboundOrderId,
        StockMovementType.RESERVATION,
        0,
        quantity,
        inventory.getOnHandQuantity(),
        inventory.getReservedQuantity(),
        occurredAt
    );
  }

  public static StockMovement shipment(
      Inventory inventory,
      long outboundOrderId,
      long quantity,
      Instant occurredAt
  ) {
    validateOrderMovement(outboundOrderId, quantity, "출고 수량은 양수여야 한다.");
    return new StockMovement(
        inventory.getProduct(),
        outboundOrderId,
        StockMovementType.SHIPMENT,
        -quantity,
        -quantity,
        inventory.getOnHandQuantity(),
        inventory.getReservedQuantity(),
        occurredAt
    );
  }

  public static StockMovement reservationRelease(
      Inventory inventory,
      long outboundOrderId,
      long quantity,
      Instant occurredAt
  ) {
    validateOrderMovement(outboundOrderId, quantity, "예약 해제 수량은 양수여야 한다.");
    return new StockMovement(
        inventory.getProduct(),
        outboundOrderId,
        StockMovementType.RESERVATION_RELEASE,
        0,
        -quantity,
        inventory.getOnHandQuantity(),
        inventory.getReservedQuantity(),
        occurredAt
    );
  }

  private static void validateOrderMovement(
      long outboundOrderId,
      long quantity,
      String quantityMessage
  ) {
    if (outboundOrderId <= 0) {
      throw new InvalidInventoryException("출고 주문 ID는 양수여야 한다.");
    }
    if (quantity <= 0) {
      throw new InvalidInventoryException(quantityMessage);
    }
  }

  private void validateState() {
    if (onHandDelta == 0 && reservedDelta == 0) {
      throw new InvalidInventoryException("재고 이동량은 0일 수 없다.");
    }
    if (onHandAfter < 0 || reservedAfter < 0 || reservedAfter > onHandAfter) {
      throw new InvalidInventoryException("재고 이동 후 수량이 재고 불변식을 위반한다.");
    }
    if (type == StockMovementType.RECEIPT && outboundOrderId != null) {
      throw new InvalidInventoryException("입고 이력에는 출고 주문 ID를 지정할 수 없다.");
    }
    if (type != StockMovementType.RECEIPT
        && (outboundOrderId == null || outboundOrderId <= 0)) {
      throw new InvalidInventoryException("주문 관련 재고 이동에는 출고 주문 ID가 필요하다.");
    }
  }

  public Long getId() {
    return id;
  }

  public Product getProduct() {
    return product;
  }

  public Long getOutboundOrderId() {
    return outboundOrderId;
  }

  public StockMovementType getType() {
    return type;
  }

  public long getOnHandDelta() {
    return onHandDelta;
  }

  public long getReservedDelta() {
    return reservedDelta;
  }

  public long getOnHandAfter() {
    return onHandAfter;
  }

  public long getReservedAfter() {
    return reservedAfter;
  }

  public long getAvailableAfter() {
    return onHandAfter - reservedAfter;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
