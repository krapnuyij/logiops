package io.github.krapnuyij.logiops.order;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.krapnuyij.logiops.inventory.Inventory;
import io.github.krapnuyij.logiops.inventory.InventoryNotFoundException;
import io.github.krapnuyij.logiops.inventory.InventoryRepository;
import io.github.krapnuyij.logiops.inventory.StockMovement;
import io.github.krapnuyij.logiops.inventory.StockMovementRepository;
import io.github.krapnuyij.logiops.product.ProductNotFoundException;
import io.github.krapnuyij.logiops.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboundOrderService {

  private final OutboundOrderRepository outboundOrderRepository;
  private final InventoryRepository inventoryRepository;
  private final StockMovementRepository stockMovementRepository;
  private final ProductRepository productRepository;
  private final Clock clock;

  public OutboundOrderService(
      OutboundOrderRepository outboundOrderRepository,
      InventoryRepository inventoryRepository,
      StockMovementRepository stockMovementRepository,
      ProductRepository productRepository,
      Clock clock
  ) {
    this.outboundOrderRepository = outboundOrderRepository;
    this.inventoryRepository = inventoryRepository;
    this.stockMovementRepository = stockMovementRepository;
    this.productRepository = productRepository;
    this.clock = clock;
  }

  @Transactional
  public OutboundOrder create(List<OrderItemCommand> itemCommands) {
    List<OrderItemCommand> sortedCommands = validateAndSort(itemCommands);
    List<Inventory> inventories = lockInventories(sortedCommands);

    for (int index = 0; index < inventories.size(); index++) {
      inventories.get(index).ensureCanReserve(sortedCommands.get(index).quantity());
    }

    Instant occurredAt = clock.instant();
    List<OutboundOrder.ItemDraft> itemDrafts = new ArrayList<>();
    for (int index = 0; index < inventories.size(); index++) {
      itemDrafts.add(new OutboundOrder.ItemDraft(
          inventories.get(index).getProduct(),
          sortedCommands.get(index).quantity()
      ));
    }

    OutboundOrder outboundOrder = outboundOrderRepository.saveAndFlush(
        OutboundOrder.createReserved(itemDrafts, occurredAt)
    );

    List<StockMovement> movements = new ArrayList<>();
    for (int index = 0; index < inventories.size(); index++) {
      Inventory inventory = inventories.get(index);
      long quantity = sortedCommands.get(index).quantity();
      inventory.reserve(quantity, occurredAt);
      movements.add(StockMovement.reservation(
          inventory,
          outboundOrder.getId(),
          quantity,
          occurredAt
      ));
    }
    stockMovementRepository.saveAllAndFlush(movements);
    return outboundOrder;
  }

  @Transactional(readOnly = true)
  public OutboundOrder getById(long orderId) {
    return outboundOrderRepository.findDetailedById(orderId)
        .orElseThrow(() -> new OutboundOrderNotFoundException(orderId));
  }

  @Transactional
  public OutboundOrder ship(long orderId) {
    OutboundOrder outboundOrder = lockOrder(orderId);
    outboundOrder.ensureCanShip();
    List<OutboundOrderItem> sortedItems = sortedItems(outboundOrder);
    List<Inventory> inventories = lockInventoriesForItems(sortedItems);

    for (int index = 0; index < inventories.size(); index++) {
      inventories.get(index).ensureCanShip(sortedItems.get(index).getQuantity());
    }

    Instant occurredAt = clock.instant();
    outboundOrder.ship(occurredAt);
    List<StockMovement> movements = new ArrayList<>();
    for (int index = 0; index < inventories.size(); index++) {
      Inventory inventory = inventories.get(index);
      long quantity = sortedItems.get(index).getQuantity();
      inventory.ship(quantity, occurredAt);
      movements.add(StockMovement.shipment(
          inventory,
          outboundOrder.getId(),
          quantity,
          occurredAt
      ));
    }
    stockMovementRepository.saveAllAndFlush(movements);
    return reloadDetailed(orderId);
  }

  @Transactional
  public OutboundOrder cancel(long orderId) {
    OutboundOrder outboundOrder = lockOrder(orderId);
    outboundOrder.ensureCanCancel();
    List<OutboundOrderItem> sortedItems = sortedItems(outboundOrder);
    List<Inventory> inventories = lockInventoriesForItems(sortedItems);

    for (int index = 0; index < inventories.size(); index++) {
      inventories.get(index).ensureCanRelease(sortedItems.get(index).getQuantity());
    }

    Instant occurredAt = clock.instant();
    outboundOrder.cancel(occurredAt);
    List<StockMovement> movements = new ArrayList<>();
    for (int index = 0; index < inventories.size(); index++) {
      Inventory inventory = inventories.get(index);
      long quantity = sortedItems.get(index).getQuantity();
      inventory.release(quantity, occurredAt);
      movements.add(StockMovement.reservationRelease(
          inventory,
          outboundOrder.getId(),
          quantity,
          occurredAt
      ));
    }
    stockMovementRepository.saveAllAndFlush(movements);
    return reloadDetailed(orderId);
  }

  private List<OrderItemCommand> validateAndSort(List<OrderItemCommand> itemCommands) {
    if (itemCommands == null || itemCommands.isEmpty()) {
      throw new InvalidOutboundOrderException("출고 주문에는 하나 이상의 항목이 필요하다.");
    }

    Set<Long> productIds = new HashSet<>();
    List<OrderItemCommand> validatedCommands = new ArrayList<>();
    for (OrderItemCommand itemCommand : itemCommands) {
      if (itemCommand == null || itemCommand.productId() <= 0 || itemCommand.quantity() <= 0) {
        throw new InvalidOutboundOrderException("상품 ID와 주문 수량은 양수여야 한다.");
      }
      if (!productIds.add(itemCommand.productId())) {
        throw new DuplicateOrderItemException(itemCommand.productId());
      }
      validatedCommands.add(itemCommand);
    }

    validatedCommands.sort(Comparator.comparingLong(OrderItemCommand::productId));
    return validatedCommands;
  }

  private List<Inventory> lockInventories(List<OrderItemCommand> sortedCommands) {
    List<Inventory> inventories = new ArrayList<>();
    for (OrderItemCommand itemCommand : sortedCommands) {
      Inventory inventory = inventoryRepository.findByProductIdForUpdate(itemCommand.productId())
          .orElseThrow(() -> missingInventoryFor(itemCommand.productId()));
      inventories.add(inventory);
    }
    return inventories;
  }

  private OutboundOrder lockOrder(long orderId) {
    return outboundOrderRepository.findByIdForUpdate(orderId)
        .orElseThrow(() -> new OutboundOrderNotFoundException(orderId));
  }

  private List<OutboundOrderItem> sortedItems(OutboundOrder outboundOrder) {
    return outboundOrder.getItems().stream()
        .sorted(Comparator.comparingLong(item -> item.getProduct().getId()))
        .toList();
  }

  private List<Inventory> lockInventoriesForItems(List<OutboundOrderItem> sortedItems) {
    List<Inventory> inventories = new ArrayList<>();
    for (OutboundOrderItem item : sortedItems) {
      long productId = item.getProduct().getId();
      Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
          .orElseThrow(() -> missingInventoryFor(productId));
      inventories.add(inventory);
    }
    return inventories;
  }

  private OutboundOrder reloadDetailed(long orderId) {
    return outboundOrderRepository.findDetailedById(orderId)
        .orElseThrow(() -> new OutboundOrderNotFoundException(orderId));
  }

  private RuntimeException missingInventoryFor(long productId) {
    if (!productRepository.existsById(productId)) {
      return new ProductNotFoundException(productId);
    }
    return new InventoryNotFoundException(productId);
  }
}
