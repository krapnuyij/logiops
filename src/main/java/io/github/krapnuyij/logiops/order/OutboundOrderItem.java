package io.github.krapnuyij.logiops.order;

import java.util.Objects;

import io.github.krapnuyij.logiops.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "outbound_order_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_outbound_order_items_order_product",
        columnNames = {"outbound_order_id", "product_id"}
    )
)
public class OutboundOrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "outbound_order_id", nullable = false, updatable = false)
  private OutboundOrder outboundOrder;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false, updatable = false)
  private Product product;

  @Column(nullable = false, updatable = false)
  private long quantity;

  protected OutboundOrderItem() {
  }

  OutboundOrderItem(OutboundOrder outboundOrder, Product product, long quantity) {
    this.outboundOrder = Objects.requireNonNull(outboundOrder, "출고 주문은 필수이다.");
    this.product = Objects.requireNonNull(product, "상품은 필수이다.");
    if (quantity <= 0) {
      throw new InvalidOutboundOrderException("주문 수량은 양수여야 한다.");
    }
    this.quantity = quantity;
  }

  public Long getId() {
    return id;
  }

  public Product getProduct() {
    return product;
  }

  public long getQuantity() {
    return quantity;
  }
}
