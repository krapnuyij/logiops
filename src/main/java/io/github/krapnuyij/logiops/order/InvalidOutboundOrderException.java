package io.github.krapnuyij.logiops.order;

import io.github.krapnuyij.logiops.common.error.ValidationException;

public class InvalidOutboundOrderException extends ValidationException {

  public InvalidOutboundOrderException(String message) {
    super("VALIDATION_FAILED", message);
  }
}
