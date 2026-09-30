package io.github.krapnuyij.logiops.inventory;

import io.github.krapnuyij.logiops.common.error.NotFoundException;

public class InventoryNotFoundException extends NotFoundException {

  public InventoryNotFoundException(long productId) {
    super("INVENTORY_NOT_FOUND", "상품의 재고를 찾을 수 없다: " + productId);
  }
}
