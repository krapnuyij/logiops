package io.github.krapnuyij.logiops.inventory;

import java.time.Instant;
import java.util.Objects;

import io.github.krapnuyij.logiops.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "inventories",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inventories_product",
        columnNames = "product_id"
    )
)
public class Inventory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false, updatable = false)
  private Product product;

  @Column(name = "on_hand_quantity", nullable = false)
  private long onHandQuantity;

  @Column(name = "reserved_quantity", nullable = false)
  private long reservedQuantity;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Inventory() {
  }

  private Inventory(Product product, Instant updatedAt) {
    this.product = Objects.requireNonNull(product, "상품은 필수이다.");
    this.onHandQuantity = 0;
    this.reservedQuantity = 0;
    this.updatedAt = Objects.requireNonNull(updatedAt, "재고 갱신 시각은 필수이다.");
  }

  public static Inventory initialize(Product product, Instant updatedAt) {
    return new Inventory(product, updatedAt);
  }

  public void receive(long quantity, Instant occurredAt) {
    validatePositiveQuantity(quantity);
    Objects.requireNonNull(occurredAt, "입고 시각은 필수이다.");

    try {
      onHandQuantity = Math.addExact(onHandQuantity, quantity);
    }
    catch (ArithmeticException exception) {
      throw new InvalidInventoryException("입고 후 현재재고가 허용 범위를 초과한다.");
    }
    updatedAt = occurredAt;
  }

  public void ensureCanReserve(long quantity) {
    validatePositiveQuantity(quantity, "예약 수량은 양수여야 한다.");
    if (quantity > getAvailableQuantity()) {
      throw new InsufficientStockException(
          product.getId(),
          quantity,
          getAvailableQuantity()
      );
    }
  }

  public void reserve(long quantity, Instant occurredAt) {
    ensureCanReserve(quantity);
    Objects.requireNonNull(occurredAt, "예약 시각은 필수이다.");
    reservedQuantity = Math.addExact(reservedQuantity, quantity);
    updatedAt = occurredAt;
  }

  public void ensureCanShip(long quantity) {
    validatePositiveQuantity(quantity, "출고 수량은 양수여야 한다.");
    ensureReservedQuantity(quantity);
  }

  public void ship(long quantity, Instant occurredAt) {
    ensureCanShip(quantity);
    Objects.requireNonNull(occurredAt, "출고 완료 시각은 필수이다.");
    onHandQuantity = Math.subtractExact(onHandQuantity, quantity);
    reservedQuantity = Math.subtractExact(reservedQuantity, quantity);
    updatedAt = occurredAt;
  }

  public void ensureCanRelease(long quantity) {
    validatePositiveQuantity(quantity, "예약 해제 수량은 양수여야 한다.");
    ensureReservedQuantity(quantity);
  }

  public void release(long quantity, Instant occurredAt) {
    ensureCanRelease(quantity);
    Objects.requireNonNull(occurredAt, "예약 해제 시각은 필수이다.");
    reservedQuantity = Math.subtractExact(reservedQuantity, quantity);
    updatedAt = occurredAt;
  }

  private void ensureReservedQuantity(long quantity) {
    if (quantity > reservedQuantity) {
      throw new InventoryReservationMismatchException(
          product.getId(),
          quantity,
          reservedQuantity
      );
    }
  }

  private void validatePositiveQuantity(long quantity) {
    validatePositiveQuantity(quantity, "입고 수량은 양수여야 한다.");
  }

  private void validatePositiveQuantity(long quantity, String message) {
    if (quantity <= 0) {
      throw new InvalidInventoryException(message);
    }
  }

  public Long getId() {
    return id;
  }

  public Product getProduct() {
    return product;
  }

  public long getOnHandQuantity() {
    return onHandQuantity;
  }

  public long getReservedQuantity() {
    return reservedQuantity;
  }

  public long getAvailableQuantity() {
    return onHandQuantity - reservedQuantity;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
