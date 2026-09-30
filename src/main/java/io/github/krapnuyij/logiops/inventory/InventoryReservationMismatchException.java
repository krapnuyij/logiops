package io.github.krapnuyij.logiops.inventory;

public class InventoryReservationMismatchException extends RuntimeException {

  public InventoryReservationMismatchException(
      Long productId,
      long requestedQuantity,
      long reservedQuantity
  ) {
    super(
        "주문 수량과 예약재고가 일치하지 않는다: productId=" + productId
            + ", requested=" + requestedQuantity
            + ", reserved=" + reservedQuantity
    );
  }
}
