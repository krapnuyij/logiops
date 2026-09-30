package io.github.krapnuyij.logiops.inventory;

import io.github.krapnuyij.logiops.common.error.ValidationException;

public class InvalidInventoryException extends ValidationException {

  public InvalidInventoryException(String message) {
    super("VALIDATION_FAILED", message);
  }
}
