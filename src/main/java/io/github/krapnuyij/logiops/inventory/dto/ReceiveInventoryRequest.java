package io.github.krapnuyij.logiops.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReceiveInventoryRequest(
    @NotNull(message = "입고 수량은 필수이다.")
    @Positive(message = "입고 수량은 양수여야 한다.")
    Long quantity
) {
}
