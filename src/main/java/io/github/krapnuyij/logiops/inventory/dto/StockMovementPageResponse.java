package io.github.krapnuyij.logiops.inventory.dto;

import java.util.List;

import io.github.krapnuyij.logiops.inventory.StockMovement;
import org.springframework.data.domain.Page;

public record StockMovementPageResponse(
    List<StockMovementResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

  public static StockMovementPageResponse from(Page<StockMovement> movements) {
    List<StockMovementResponse> content = movements.getContent().stream()
        .map(StockMovementResponse::from)
        .toList();
    return new StockMovementPageResponse(
        content,
        movements.getNumber(),
        movements.getSize(),
        movements.getTotalElements(),
        movements.getTotalPages()
    );
  }
}
