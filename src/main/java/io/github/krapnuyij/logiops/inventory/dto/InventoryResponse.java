package io.github.krapnuyij.logiops.inventory.dto;

import java.time.Instant;

import io.github.krapnuyij.logiops.inventory.Inventory;

public record InventoryResponse(
    Long productId,
    long onHandQuantity,
    long reservedQuantity,
    long availableQuantity,
    Instant updatedAt
) {

  public static InventoryResponse from(Inventory inventory) {
    return new InventoryResponse(
        inventory.getProduct().getId(),
        inventory.getOnHandQuantity(),
        inventory.getReservedQuantity(),
        inventory.getAvailableQuantity(),
        inventory.getUpdatedAt()
    );
  }
}
