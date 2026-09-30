package io.github.krapnuyij.logiops.order;

public record OrderItemCommand(long productId, long quantity) {
}
