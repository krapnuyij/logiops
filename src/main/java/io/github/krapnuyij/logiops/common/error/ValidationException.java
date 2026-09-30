package io.github.krapnuyij.logiops.common.error;

public abstract class ValidationException extends ApplicationException {

  private final String field;

  protected ValidationException(String errorCode, String message) {
    this(errorCode, null, message);
  }

  protected ValidationException(String errorCode, String field, String message) {
    super(errorCode, message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
