package io.github.krapnuyij.logiops.common.error;

public abstract class ConflictException extends ApplicationException {

  protected ConflictException(String errorCode, String message) {
    super(errorCode, message);
  }

  protected ConflictException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
