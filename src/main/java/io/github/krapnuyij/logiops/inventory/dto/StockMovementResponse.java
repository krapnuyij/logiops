package io.github.krapnuyij.logiops.inventory.dto;

import java.time.Instant;

import io.github.krapnuyij.logiops.inventory.StockMovement;
import io.github.krapnuyij.logiops.inventory.StockMovementType;

public record StockMovementResponse(
    Long id,
    Long productId,
    Long orderId,
    StockMovementType type,
    long onHandDelta,
    long reservedDelta,
    long onHandAfter,
    long reservedAfter,
    long availableAfter,
    Instant occurredAt
) {

  public static StockMovementResponse from(StockMovement movement) {
    return new StockMovementResponse(
        movement.getId(),
        movement.getProduct().getId(),
        movement.getOutboundOrderId(),
        movement.getType(),
        movement.getOnHandDelta(),
        movement.getReservedDelta(),
        movement.getOnHandAfter(),
        movement.getReservedAfter(),
        movement.getAvailableAfter(),
        movement.getOccurredAt()
    );
  }
}
