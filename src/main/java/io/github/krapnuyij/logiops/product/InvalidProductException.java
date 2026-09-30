package io.github.krapnuyij.logiops.product;

import io.github.krapnuyij.logiops.common.error.ValidationException;

public class InvalidProductException extends ValidationException {

  public InvalidProductException(String field, String message) {
    super("VALIDATION_FAILED", field, message);
  }
}
