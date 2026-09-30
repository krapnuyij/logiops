package io.github.krapnuyij.logiops.order;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.krapnuyij.logiops.inventory.InsufficientStockException;
import io.github.krapnuyij.logiops.inventory.Inventory;
import io.github.krapnuyij.logiops.inventory.InventoryNotFoundException;
import io.github.krapnuyij.logiops.inventory.InventoryRepository;
import io.github.krapnuyij.logiops.inventory.InventoryService;
import io.github.krapnuyij.logiops.inventory.StockMovement;
import io.github.krapnuyij.logiops.inventory.StockMovementRepository;
import io.github.krapnuyij.logiops.inventory.StockMovementType;
import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductNotFoundException;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OutboundOrderServiceIntegrationTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @Autowired
  private OutboundOrderRepository outboundOrderRepository;

  @Autowired
  private OutboundOrderService outboundOrderService;

  @BeforeEach
  void clearData() {
    deleteAllData();
  }

  @AfterEach
  void cleanUpData() {
    deleteAllData();
  }

  @Test
  void createsMultiItemOrderAndReservesEveryInventory() {
    Product firstProduct = productWithStock("SKU-001", 10);
    Product secondProduct = productWithStock("SKU-002", 8);

    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(secondProduct.getId(), 2),
        new OrderItemCommand(firstProduct.getId(), 4)
    ));

    assertThat(order.getStatus()).isEqualTo(OutboundOrderStatus.RESERVED);
    assertThat(order.getItems()).extracting(item -> item.getProduct().getId())
        .containsExactly(firstProduct.getId(), secondProduct.getId());
    assertThat(inventoryService.getByProductId(firstProduct.getId()).getReservedQuantity())
        .isEqualTo(4);
    assertThat(inventoryService.getByProductId(secondProduct.getId()).getReservedQuantity())
        .isEqualTo(2);

    List<StockMovement> reservations = stockMovementRepository.findAll().stream()
        .filter(movement -> movement.getType() == StockMovementType.RESERVATION)
        .toList();
    assertThat(reservations).hasSize(2).allSatisfy(movement -> {
      assertThat(movement.getOutboundOrderId()).isEqualTo(order.getId());
      assertThat(movement.getOccurredAt()).isEqualTo(order.getCreatedAt());
    });
  }

  @Test
  void rollsBackWholeOrderWhenOneProductHasInsufficientStock() {
    Product firstProduct = productWithStock("SKU-001", 10);
    Product secondProduct = productWithStock("SKU-002", 1);

    assertThatThrownBy(() -> outboundOrderService.create(List.of(
        new OrderItemCommand(firstProduct.getId(), 5),
        new OrderItemCommand(secondProduct.getId(), 2)
    ))).isInstanceOf(InsufficientStockException.class)
        .extracting("errorCode")
        .isEqualTo("INSUFFICIENT_STOCK");

    assertThat(outboundOrderRepository.count()).isZero();
    assertThat(inventoryService.getByProductId(firstProduct.getId()).getReservedQuantity()).isZero();
    assertThat(inventoryService.getByProductId(secondProduct.getId()).getReservedQuantity()).isZero();
    assertThat(stockMovementRepository.findAll()).allMatch(
        movement -> movement.getType() == StockMovementType.RECEIPT
    );
  }

  @Test
  void rejectsDuplicateProductBeforeCreatingOrder() {
    Product product = productWithStock("SKU-001", 10);

    assertThatThrownBy(() -> outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 1),
        new OrderItemCommand(product.getId(), 2)
    ))).isInstanceOf(DuplicateOrderItemException.class)
        .extracting("errorCode")
        .isEqualTo("DUPLICATE_ORDER_ITEM");
    assertThat(outboundOrderRepository.count()).isZero();
  }

  @Test
  void distinguishesMissingProductFromMissingInventory() {
    assertThatThrownBy(() -> outboundOrderService.create(List.of(
        new OrderItemCommand(999L, 1)
    ))).isInstanceOf(ProductNotFoundException.class)
        .extracting("errorCode")
        .isEqualTo("PRODUCT_NOT_FOUND");

    Product productWithoutInventory = productRepository.saveAndFlush(
        Product.create("SKU-ORPHAN", "상품", Instant.parse("2026-09-30T01:00:00Z"))
    );
    assertThatThrownBy(() -> outboundOrderService.create(List.of(
        new OrderItemCommand(productWithoutInventory.getId(), 1)
    ))).isInstanceOf(InventoryNotFoundException.class)
        .extracting("errorCode")
        .isEqualTo("INVENTORY_NOT_FOUND");
  }

  @Test
  void allowsOnlyOneCompetingOrderWhenCombinedRequestExceedsStock() throws Exception {
    Product product = productWithStock("SKU-001", 10);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try {
      Future<Boolean> first = executor.submit(orderTask(product.getId(), ready, start));
      Future<Boolean> second = executor.submit(orderTask(product.getId(), ready, start));

      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(List.of(
          first.get(10, TimeUnit.SECONDS),
          second.get(10, TimeUnit.SECONDS)
      )).containsExactlyInAnyOrder(true, false);

      Inventory inventory = inventoryService.getByProductId(product.getId());
      assertThat(inventory.getOnHandQuantity()).isEqualTo(10);
      assertThat(inventory.getReservedQuantity()).isEqualTo(7);
      assertThat(inventory.getAvailableQuantity()).isEqualTo(3);
      assertThat(outboundOrderRepository.count()).isEqualTo(1);
      assertThat(stockMovementRepository.findAll().stream()
          .filter(movement -> movement.getType() == StockMovementType.RESERVATION))
          .hasSize(1);
    }
    finally {
      start.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void shipsMultiItemOrderAndCreatesShipmentMovements() {
    Product firstProduct = productWithStock("SHIP-001", 10);
    Product secondProduct = productWithStock("SHIP-002", 8);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(secondProduct.getId(), 2),
        new OrderItemCommand(firstProduct.getId(), 4)
    ));

    OutboundOrder shipped = outboundOrderService.ship(order.getId());

    assertThat(shipped.getStatus()).isEqualTo(OutboundOrderStatus.SHIPPED);
    assertThat(shipped.getShippedAt()).isNotNull();
    assertThat(shipped.getCancelledAt()).isNull();
    assertInventory(firstProduct.getId(), 6, 0, 6, shipped.getShippedAt());
    assertInventory(secondProduct.getId(), 6, 0, 6, shipped.getShippedAt());

    List<StockMovement> movements = movements(order.getId(), StockMovementType.SHIPMENT);
    assertThat(movements).hasSize(2).allSatisfy(movement -> {
      assertThat(movement.getOnHandDelta()).isNegative();
      assertThat(movement.getReservedDelta()).isEqualTo(movement.getOnHandDelta());
      assertThat(movement.getOccurredAt()).isEqualTo(shipped.getShippedAt());
    });
  }

  @Test
  void cancelsMultiItemOrderAndCreatesReservationReleaseMovements() {
    Product firstProduct = productWithStock("CANCEL-001", 10);
    Product secondProduct = productWithStock("CANCEL-002", 8);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(secondProduct.getId(), 2),
        new OrderItemCommand(firstProduct.getId(), 4)
    ));

    OutboundOrder cancelled = outboundOrderService.cancel(order.getId());

    assertThat(cancelled.getStatus()).isEqualTo(OutboundOrderStatus.CANCELLED);
    assertThat(cancelled.getShippedAt()).isNull();
    assertThat(cancelled.getCancelledAt()).isNotNull();
    assertInventory(firstProduct.getId(), 10, 0, 10, cancelled.getCancelledAt());
    assertInventory(secondProduct.getId(), 8, 0, 8, cancelled.getCancelledAt());

    List<StockMovement> movements = movements(
        order.getId(),
        StockMovementType.RESERVATION_RELEASE
    );
    assertThat(movements).hasSize(2).allSatisfy(movement -> {
      assertThat(movement.getOnHandDelta()).isZero();
      assertThat(movement.getReservedDelta()).isNegative();
      assertThat(movement.getOccurredAt()).isEqualTo(cancelled.getCancelledAt());
    });
  }

  @Test
  void rejectsEveryTransitionFromFinalStatesWithoutFurtherStockChanges() {
    Product product = productWithStock("FINAL-STATE", 20);
    OutboundOrder shippedOrder = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ));
    outboundOrderService.ship(shippedOrder.getId());

    assertThatThrownBy(() -> outboundOrderService.ship(shippedOrder.getId()))
        .isInstanceOf(InvalidOrderStateException.class)
        .extracting("errorCode")
        .isEqualTo("INVALID_ORDER_STATE");
    assertThatThrownBy(() -> outboundOrderService.cancel(shippedOrder.getId()))
        .isInstanceOf(InvalidOrderStateException.class);

    OutboundOrder cancelledOrder = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 3)
    ));
    outboundOrderService.cancel(cancelledOrder.getId());

    assertThatThrownBy(() -> outboundOrderService.ship(cancelledOrder.getId()))
        .isInstanceOf(InvalidOrderStateException.class);
    assertThatThrownBy(() -> outboundOrderService.cancel(cancelledOrder.getId()))
        .isInstanceOf(InvalidOrderStateException.class);

    Inventory inventory = inventoryService.getByProductId(product.getId());
    assertThat(inventory.getOnHandQuantity()).isEqualTo(16);
    assertThat(inventory.getReservedQuantity()).isZero();
    assertThat(movements(shippedOrder.getId(), StockMovementType.SHIPMENT)).hasSize(1);
    assertThat(movements(cancelledOrder.getId(), StockMovementType.RESERVATION_RELEASE)).hasSize(1);
  }

  @Test
  void returnsNotFoundWhenFinalizingUnknownOrder() {
    assertThatThrownBy(() -> outboundOrderService.ship(999L))
        .isInstanceOf(OutboundOrderNotFoundException.class)
        .extracting("errorCode")
        .isEqualTo("ORDER_NOT_FOUND");
    assertThatThrownBy(() -> outboundOrderService.cancel(999L))
        .isInstanceOf(OutboundOrderNotFoundException.class);
  }

  private Product productWithStock(String sku, long quantity) {
    Product product = productService.create(sku, "상품");
    inventoryService.receive(product.getId(), quantity);
    return product;
  }

  private Callable<Boolean> orderTask(
      long productId,
      CountDownLatch ready,
      CountDownLatch start
  ) {
    return () -> {
      ready.countDown();
      if (!start.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시 주문 시작 신호를 받지 못했다.");
      }
      try {
        outboundOrderService.create(List.of(new OrderItemCommand(productId, 7)));
        return true;
      }
      catch (InsufficientStockException exception) {
        return false;
      }
    };
  }

  private List<StockMovement> movements(long orderId, StockMovementType type) {
    return stockMovementRepository.findAll().stream()
        .filter(movement -> movement.getOutboundOrderId() != null)
        .filter(movement -> movement.getOutboundOrderId() == orderId)
        .filter(movement -> movement.getType() == type)
        .toList();
  }

  private void assertInventory(
      long productId,
      long onHandQuantity,
      long reservedQuantity,
      long availableQuantity,
      Instant updatedAt
  ) {
    Inventory inventory = inventoryService.getByProductId(productId);
    assertThat(inventory.getOnHandQuantity()).isEqualTo(onHandQuantity);
    assertThat(inventory.getReservedQuantity()).isEqualTo(reservedQuantity);
    assertThat(inventory.getAvailableQuantity()).isEqualTo(availableQuantity);
    assertThat(inventory.getUpdatedAt()).isEqualTo(updatedAt);
  }

  private void deleteAllData() {
    stockMovementRepository.deleteAll();
    outboundOrderRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }
}
