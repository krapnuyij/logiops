package io.github.krapnuyij.logiops.product;

import java.time.Clock;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductInventoryInitializer productInventoryInitializer;
  private final Clock clock;

  public ProductService(
      ProductRepository productRepository,
      ProductInventoryInitializer productInventoryInitializer,
      Clock clock
  ) {
    this.productRepository = productRepository;
    this.productInventoryInitializer = productInventoryInitializer;
    this.clock = clock;
  }

  @Transactional
  public Product create(String sku, String name) {
    Product product = Product.create(sku, name, clock.instant());
    if (productRepository.existsBySku(product.getSku())) {
      throw new DuplicateSkuException(product.getSku());
    }

    Product savedProduct;
    try {
      savedProduct = productRepository.saveAndFlush(product);
    }
    catch (DataIntegrityViolationException exception) {
      throw new DuplicateSkuException(product.getSku(), exception);
    }

    productInventoryInitializer.initialize(savedProduct);
    return savedProduct;
  }

  @Transactional(readOnly = true)
  public Product getById(long productId) {
    return productRepository.findById(productId)
        .orElseThrow(() -> new ProductNotFoundException(productId));
  }

  @Transactional(readOnly = true)
  public Page<Product> getAll(int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    return productRepository.findAll(pageRequest);
  }
}
