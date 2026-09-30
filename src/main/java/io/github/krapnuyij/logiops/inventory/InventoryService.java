package io.github.krapnuyij.logiops.inventory;

import java.time.Clock;
import java.time.Instant;

import io.github.krapnuyij.logiops.product.ProductNotFoundException;
import io.github.krapnuyij.logiops.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

  private final InventoryRepository inventoryRepository;
  private final StockMovementRepository stockMovementRepository;
  private final ProductRepository productRepository;
  private final Clock clock;

  public InventoryService(
      InventoryRepository inventoryRepository,
      StockMovementRepository stockMovementRepository,
      ProductRepository productRepository,
      Clock clock
  ) {
    this.inventoryRepository = inventoryRepository;
    this.stockMovementRepository = stockMovementRepository;
    this.productRepository = productRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Inventory getByProductId(long productId) {
    return inventoryRepository.findByProductId(productId)
        .orElseThrow(() -> missingInventoryFor(productId));
  }

  @Transactional
  public Inventory receive(long productId, long quantity) {
    Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
        .orElseThrow(() -> missingInventoryFor(productId));
    Instant occurredAt = clock.instant();

    inventory.receive(quantity, occurredAt);
    stockMovementRepository.save(StockMovement.receipt(inventory, quantity, occurredAt));
    return inventory;
  }

  private RuntimeException missingInventoryFor(long productId) {
    if (!productRepository.existsById(productId)) {
      return new ProductNotFoundException(productId);
    }
    return new InventoryNotFoundException(productId);
  }
}
