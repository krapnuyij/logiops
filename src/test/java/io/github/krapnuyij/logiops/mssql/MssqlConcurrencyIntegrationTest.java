package io.github.krapnuyij.logiops.mssql;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.krapnuyij.logiops.inventory.InsufficientStockException;
import io.github.krapnuyij.logiops.inventory.Inventory;
import io.github.krapnuyij.logiops.inventory.InventoryRepository;
import io.github.krapnuyij.logiops.inventory.InventoryService;
import io.github.krapnuyij.logiops.inventory.StockMovementRepository;
import io.github.krapnuyij.logiops.inventory.StockMovementType;
import io.github.krapnuyij.logiops.order.InvalidOrderStateException;
import io.github.krapnuyij.logiops.order.OrderItemCommand;
import io.github.krapnuyij.logiops.order.OutboundOrder;
import io.github.krapnuyij.logiops.order.OutboundOrderRepository;
import io.github.krapnuyij.logiops.order.OutboundOrderService;
import io.github.krapnuyij.logiops.order.OutboundOrderStatus;
import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("mssql")
@Tag("mssql")
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class MssqlConcurrencyIntegrationTest {

  private static final long READY_TIMEOUT_SECONDS = 10;
  private static final long RESULT_TIMEOUT_SECONDS = 20;
  private static final long TERMINATION_TIMEOUT_SECONDS = 10;

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @Autowired
  private OutboundOrderService outboundOrderService;

  @Autowired
  private OutboundOrderRepository outboundOrderRepository;

  @BeforeEach
  void clearData() {
    deleteAllData();
  }

  @AfterEach
  void cleanUpData() {
    deleteAllData();
  }

  @Test
  void preventsOverReservationWhenOrdersCompete() throws Exception {
    Product product = productWithStock("MSSQL-OVER-RESERVE", 10);

    List<ReservationResult> results = runConcurrently(
        () -> reserve(product.getId(), 7),
        () -> reserve(product.getId(), 7)
    );

    assertThat(results).containsExactlyInAnyOrder(
        ReservationResult.RESERVED,
        ReservationResult.INSUFFICIENT_STOCK
    );
    assertInventory(product.getId(), 10, 7, 3);
    assertThat(outboundOrderRepository.count()).isEqualTo(1);
    assertThat(movementCount(StockMovementType.RESERVATION)).isEqualTo(1);
  }

  @Test
  void allowsOnlyOneFinalStateWhenShipAndCancelCompete() throws Exception {
    Product product = productWithStock("MSSQL-SHIP-CANCEL", 10);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ));

    List<FinalizationResult> results = runConcurrently(
        () -> ship(order.getId()),
        () -> cancel(order.getId())
    );

    assertThat(results).filteredOn(result -> result == FinalizationResult.INVALID_STATE)
        .hasSize(1);
    OutboundOrderStatus status = outboundOrderService.getById(order.getId()).getStatus();
    if (status == OutboundOrderStatus.SHIPPED) {
      assertThat(results).contains(FinalizationResult.SHIPPED);
      assertInventory(product.getId(), 6, 0, 6);
      assertThat(movementCount(order.getId(), StockMovementType.SHIPMENT)).isEqualTo(1);
      assertThat(movementCount(order.getId(), StockMovementType.RESERVATION_RELEASE)).isZero();
    }
    else {
      assertThat(status).isEqualTo(OutboundOrderStatus.CANCELLED);
      assertThat(results).contains(FinalizationResult.CANCELLED);
      assertInventory(product.getId(), 10, 0, 10);
      assertThat(movementCount(order.getId(), StockMovementType.SHIPMENT)).isZero();
      assertThat(movementCount(
          order.getId(),
          StockMovementType.RESERVATION_RELEASE
      )).isEqualTo(1);
    }
  }

  @Test
  void avoidsDeadlockForOppositeMultiItemOrderInput() throws Exception {
    Product firstProduct = productWithStock("MSSQL-LOCK-001", 20);
    Product secondProduct = productWithStock("MSSQL-LOCK-002", 20);

    List<Long> orderIds = runConcurrently(
        () -> outboundOrderService.create(List.of(
            new OrderItemCommand(secondProduct.getId(), 3),
            new OrderItemCommand(firstProduct.getId(), 4)
        )).getId(),
        () -> outboundOrderService.create(List.of(
            new OrderItemCommand(firstProduct.getId(), 5),
            new OrderItemCommand(secondProduct.getId(), 2)
        )).getId()
    );

    assertThat(orderIds).hasSize(2).doesNotHaveDuplicates();
    assertInventory(firstProduct.getId(), 20, 9, 11);
    assertInventory(secondProduct.getId(), 20, 5, 15);
    assertThat(outboundOrderRepository.count()).isEqualTo(2);
    assertThat(movementCount(StockMovementType.RESERVATION)).isEqualTo(4);
  }

  private ReservationResult reserve(long productId, long quantity) {
    try {
      outboundOrderService.create(List.of(new OrderItemCommand(productId, quantity)));
      return ReservationResult.RESERVED;
    }
    catch (InsufficientStockException exception) {
      return ReservationResult.INSUFFICIENT_STOCK;
    }
  }

  private FinalizationResult ship(long orderId) {
    try {
      outboundOrderService.ship(orderId);
      return FinalizationResult.SHIPPED;
    }
    catch (InvalidOrderStateException exception) {
      return FinalizationResult.INVALID_STATE;
    }
  }

  private FinalizationResult cancel(long orderId) {
    try {
      outboundOrderService.cancel(orderId);
      return FinalizationResult.CANCELLED;
    }
    catch (InvalidOrderStateException exception) {
      return FinalizationResult.INVALID_STATE;
    }
  }

  private <T> List<T> runConcurrently(
      Callable<T> firstTask,
      Callable<T> secondTask
  ) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try {
      Future<T> first = executor.submit(synchronizedTask(firstTask, ready, start));
      Future<T> second = executor.submit(synchronizedTask(secondTask, ready, start));

      assertThat(ready.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return List.of(
          first.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
          second.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      );
    }
    finally {
      start.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(
          TERMINATION_TIMEOUT_SECONDS,
          TimeUnit.SECONDS
      )).isTrue();
    }
  }

  private <T> Callable<T> synchronizedTask(
      Callable<T> task,
      CountDownLatch ready,
      CountDownLatch start
  ) {
    return () -> {
      ready.countDown();
      if (!start.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시성 테스트 시작 신호를 받지 못했다.");
      }
      return task.call();
    };
  }

  private Product productWithStock(String sku, long quantity) {
    Product product = productService.create(sku, "MSSQL 동시성 상품");
    inventoryService.receive(product.getId(), quantity);
    return product;
  }

  private void assertInventory(
      long productId,
      long onHandQuantity,
      long reservedQuantity,
      long availableQuantity
  ) {
    Inventory inventory = inventoryService.getByProductId(productId);
    assertThat(inventory.getOnHandQuantity()).isEqualTo(onHandQuantity);
    assertThat(inventory.getReservedQuantity()).isEqualTo(reservedQuantity);
    assertThat(inventory.getAvailableQuantity()).isEqualTo(availableQuantity);
  }

  private long movementCount(StockMovementType type) {
    return stockMovementRepository.findAll().stream()
        .filter(movement -> movement.getType() == type)
        .count();
  }

  private long movementCount(long orderId, StockMovementType type) {
    return stockMovementRepository.findAll().stream()
        .filter(movement -> movement.getOutboundOrderId() != null)
        .filter(movement -> movement.getOutboundOrderId() == orderId)
        .filter(movement -> movement.getType() == type)
        .count();
  }

  private void deleteAllData() {
    stockMovementRepository.deleteAll();
    outboundOrderRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }

  private enum ReservationResult {
    RESERVED,
    INSUFFICIENT_STOCK
  }

  private enum FinalizationResult {
    SHIPPED,
    CANCELLED,
    INVALID_STATE
  }
}
