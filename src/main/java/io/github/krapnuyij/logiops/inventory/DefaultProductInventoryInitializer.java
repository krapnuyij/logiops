package io.github.krapnuyij.logiops.inventory;

import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductInventoryInitializer;
import org.springframework.stereotype.Component;

@Component
public class DefaultProductInventoryInitializer implements ProductInventoryInitializer {

  private final InventoryRepository inventoryRepository;

  public DefaultProductInventoryInitializer(InventoryRepository inventoryRepository) {
    this.inventoryRepository = inventoryRepository;
  }

  @Override
  public void initialize(Product product) {
    Inventory inventory = Inventory.initialize(product, product.getCreatedAt());
    inventoryRepository.saveAndFlush(inventory);
  }
}
