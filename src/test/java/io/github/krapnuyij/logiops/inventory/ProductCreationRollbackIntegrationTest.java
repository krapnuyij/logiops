package io.github.krapnuyij.logiops.inventory;

import io.github.krapnuyij.logiops.product.ProductInventoryInitializer;
import io.github.krapnuyij.logiops.product.ProductRepository;
import io.github.krapnuyij.logiops.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class ProductCreationRollbackIntegrationTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @MockitoBean
  private ProductInventoryInitializer productInventoryInitializer;

  @Test
  void rollsBackProductWhenInventoryInitializationFails() {
    doThrow(new IllegalStateException("재고 초기화 실패"))
        .when(productInventoryInitializer)
        .initialize(any());

    assertThatThrownBy(() -> productService.create("SKU-ROLLBACK", "상품"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("재고 초기화 실패");
    assertThat(productRepository.findAll()).isEmpty();
  }
}
