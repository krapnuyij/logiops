package io.github.krapnuyij.logiops.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMovementService {

  private static final Sort NEWEST_FIRST = Sort.by(
      Sort.Order.desc("occurredAt"),
      Sort.Order.desc("id")
  );

  private final StockMovementRepository stockMovementRepository;

  public StockMovementService(StockMovementRepository stockMovementRepository) {
    this.stockMovementRepository = stockMovementRepository;
  }

  @Transactional(readOnly = true)
  public Page<StockMovement> getAll(
      Long productId,
      Long outboundOrderId,
      StockMovementType type,
      int page,
      int size
  ) {
    PageRequest pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
    return stockMovementRepository.findAllByFilters(
        productId,
        outboundOrderId,
        type,
        pageRequest
    );
  }
}
