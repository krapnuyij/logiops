package io.github.krapnuyij.logiops.product.dto;

import java.util.List;

import io.github.krapnuyij.logiops.product.Product;
import org.springframework.data.domain.Page;

public record ProductPageResponse(
    List<ProductResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

  public static ProductPageResponse from(Page<Product> products) {
    List<ProductResponse> content = products.getContent().stream()
        .map(ProductResponse::from)
        .toList();
    return new ProductPageResponse(
        content,
        products.getNumber(),
        products.getSize(),
        products.getTotalElements(),
        products.getTotalPages()
    );
  }
}
