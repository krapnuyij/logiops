package io.github.krapnuyij.logiops.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.github.krapnuyij.logiops.product.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbound_orders")
public class OutboundOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private OutboundOrderStatus status;

  @OneToMany(
      mappedBy = "outboundOrder",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY
  )
  private List<OutboundOrderItem> items = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "shipped_at")
  private Instant shippedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  protected OutboundOrder() {
  }

  private OutboundOrder(List<ItemDraft> itemDrafts, Instant createdAt) {
    if (itemDrafts == null || itemDrafts.isEmpty()) {
      throw new InvalidOutboundOrderException("출고 주문에는 하나 이상의 항목이 필요하다.");
    }

    this.status = OutboundOrderStatus.RESERVED;
    this.createdAt = Objects.requireNonNull(createdAt, "주문 생성 시각은 필수이다.");
    this.shippedAt = null;
    this.cancelledAt = null;

    Set<String> productSkus = new HashSet<>();
    for (ItemDraft itemDraft : itemDrafts) {
      if (itemDraft == null || itemDraft.product() == null) {
        throw new InvalidOutboundOrderException("주문 상품은 필수이다.");
      }
      if (!productSkus.add(itemDraft.product().getSku())) {
        throw new DuplicateOrderItemException(itemDraft.product().getId());
      }
      items.add(new OutboundOrderItem(this, itemDraft.product(), itemDraft.quantity()));
    }
  }

  static OutboundOrder createReserved(List<ItemDraft> itemDrafts, Instant createdAt) {
    return new OutboundOrder(itemDrafts, createdAt);
  }

  public void ensureCanShip() {
    ensureReserved("SHIP");
  }

  public void ship(Instant shippedAt) {
    ensureCanShip();
    this.shippedAt = Objects.requireNonNull(shippedAt, "출고 완료 시각은 필수이다.");
    this.status = OutboundOrderStatus.SHIPPED;
  }

  public void ensureCanCancel() {
    ensureReserved("CANCEL");
  }

  public void cancel(Instant cancelledAt) {
    ensureCanCancel();
    this.cancelledAt = Objects.requireNonNull(cancelledAt, "주문 취소 시각은 필수이다.");
    this.status = OutboundOrderStatus.CANCELLED;
  }

  private void ensureReserved(String action) {
    if (status != OutboundOrderStatus.RESERVED) {
      throw new InvalidOrderStateException(id, status, action);
    }
  }

  record ItemDraft(Product product, long quantity) {
  }

  public Long getId() {
    return id;
  }

  public OutboundOrderStatus getStatus() {
    return status;
  }

  public List<OutboundOrderItem> getItems() {
    return List.copyOf(items);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getShippedAt() {
    return shippedAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }
}
