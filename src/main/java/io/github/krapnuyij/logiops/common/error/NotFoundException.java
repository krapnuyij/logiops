package io.github.krapnuyij.logiops.common.error;

public abstract class NotFoundException extends ApplicationException {

  protected NotFoundException(String errorCode, String message) {
    super(errorCode, message);
  }
}
