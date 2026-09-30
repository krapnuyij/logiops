package io.github.krapnuyij.logiops.common.error;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@Schema(description = "LogiOps API 오류 응답")
public final class ApiProblemDetail extends ProblemDetail {

  private final String errorCode;
  private final List<ApiFieldError> fieldErrors;

  private ApiProblemDetail(
      HttpStatus status,
      String errorCode,
      String detail,
      List<ApiFieldError> fieldErrors
  ) {
    super(status.value());
    setType(URI.create("about:blank"));
    setTitle(status.getReasonPhrase());
    setDetail(detail);
    this.errorCode = errorCode;
    this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
  }

  public static ApiProblemDetail of(HttpStatus status, String errorCode, String detail) {
    return new ApiProblemDetail(status, errorCode, detail, List.of());
  }

  public static ApiProblemDetail of(
      HttpStatus status,
      String errorCode,
      String detail,
      List<ApiFieldError> fieldErrors
  ) {
    return new ApiProblemDetail(status, errorCode, detail, fieldErrors);
  }

  @Schema(description = "애플리케이션 오류 코드", example = "VALIDATION_FAILED")
  public String getErrorCode() {
    return errorCode;
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Schema(description = "필드 단위 검증 오류 목록")
  public List<ApiFieldError> getFieldErrors() {
    return fieldErrors;
  }

  @Override
  @Schema(hidden = true)
  public Map<String, Object> getProperties() {
    return super.getProperties();
  }
}
