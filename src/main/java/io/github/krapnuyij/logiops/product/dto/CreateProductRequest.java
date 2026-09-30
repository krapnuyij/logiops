package io.github.krapnuyij.logiops.product.dto;

import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
    @NotNull(message = "SKU는 필수이다.") String sku,
    @NotNull(message = "상품명은 필수이다.") String name
) {
}
