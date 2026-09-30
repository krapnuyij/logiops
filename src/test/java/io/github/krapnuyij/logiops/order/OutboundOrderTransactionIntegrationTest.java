package io.github.krapnuyij.logiops.order;

import java.util.List;

import io.github.krapnuyij.logiops.inventory.Inventory;
import io.github.krapnuyij.logiops.inventory.InventoryRepository;
import io.github.krapnuyij.logiops.inventory.InventoryService;
import io.github.krapnuyij.logiops.inventory.StockMovementRepository;
import io.github.krapnuyij.logiops.product.Product;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class OutboundOrderTransactionIntegrationTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private OutboundOrderRepository outboundOrderRepository;

  @Autowired
  private OutboundOrderService outboundOrderService;

  @MockitoBean
  private StockMovementRepository stockMovementRepository;

  @BeforeEach
  void clearData() {
    deleteAllData();
  }

  @AfterEach
  void cleanUpData() {
    deleteAllData();
  }

  @Test
  void rollsBackOrderAndReservationWhenMovementPersistenceFails() {
    Product product = productService.create("ORDER-ROLLBACK", "상품");
    inventoryService.receive(product.getId(), 10);
    doThrow(new IllegalStateException("이력 저장 실패"))
        .when(stockMovementRepository)
        .saveAllAndFlush(anyList());

    assertThatThrownBy(() -> outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ))).isInstanceOf(IllegalStateException.class)
        .hasMessage("이력 저장 실패");

    assertThat(outboundOrderRepository.findAll()).isEmpty();
    Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
    assertThat(inventory.getOnHandQuantity()).isEqualTo(10);
    assertThat(inventory.getReservedQuantity()).isZero();
  }

  @Test
  void rollsBackShipmentAndOrderStateWhenMovementPersistenceFails() {
    Product product = productService.create("SHIP-ROLLBACK", "상품");
    inventoryService.receive(product.getId(), 10);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ));
    doThrow(new IllegalStateException("출고 이력 저장 실패"))
        .when(stockMovementRepository)
        .saveAllAndFlush(anyList());

    assertThatThrownBy(() -> outboundOrderService.ship(order.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("출고 이력 저장 실패");

    OutboundOrder unchanged = outboundOrderRepository.findDetailedById(order.getId()).orElseThrow();
    assertThat(unchanged.getStatus()).isEqualTo(OutboundOrderStatus.RESERVED);
    assertThat(unchanged.getShippedAt()).isNull();
    Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
    assertThat(inventory.getOnHandQuantity()).isEqualTo(10);
    assertThat(inventory.getReservedQuantity()).isEqualTo(4);
  }

  @Test
  void rollsBackCancellationAndOrderStateWhenMovementPersistenceFails() {
    Product product = productService.create("CANCEL-ROLLBACK", "상품");
    inventoryService.receive(product.getId(), 10);
    OutboundOrder order = outboundOrderService.create(List.of(
        new OrderItemCommand(product.getId(), 4)
    ));
    doThrow(new IllegalStateException("예약 해제 이력 저장 실패"))
        .when(stockMovementRepository)
        .saveAllAndFlush(anyList());

    assertThatThrownBy(() -> outboundOrderService.cancel(order.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("예약 해제 이력 저장 실패");

    OutboundOrder unchanged = outboundOrderRepository.findDetailedById(order.getId()).orElseThrow();
    assertThat(unchanged.getStatus()).isEqualTo(OutboundOrderStatus.RESERVED);
    assertThat(unchanged.getCancelledAt()).isNull();
    Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
    assertThat(inventory.getOnHandQuantity()).isEqualTo(10);
    assertThat(inventory.getReservedQuantity()).isEqualTo(4);
  }

  private void deleteAllData() {
    outboundOrderRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
  }
}
