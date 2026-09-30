package io.github.krapnuyij.logiops.common.error;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  ApiProblemDetail handleNotFound(NotFoundException exception) {
    return ApiProblemDetail.of(
        HttpStatus.NOT_FOUND,
        exception.getErrorCode(),
        exception.getMessage()
    );
  }

  @ExceptionHandler(ConflictException.class)
  ApiProblemDetail handleConflict(ConflictException exception) {
    return ApiProblemDetail.of(
        HttpStatus.CONFLICT,
        exception.getErrorCode(),
        exception.getMessage()
    );
  }

  @ExceptionHandler(ValidationException.class)
  ApiProblemDetail handleDomainValidation(ValidationException exception) {
    List<ApiFieldError> fieldErrors = exception.getField() == null
        ? List.of()
        : List.of(new ApiFieldError(exception.getField(), exception.getMessage()));
    return ApiProblemDetail.of(
        HttpStatus.BAD_REQUEST,
        exception.getErrorCode(),
        exception.getMessage(),
        fieldErrors
    );
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    List<ApiFieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
        .map(this::toApiFieldError)
        .toList();
    return new ResponseEntity<>(validationProblem(fieldErrors), headers, status);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    List<ApiFieldError> fieldErrors = new ArrayList<>();
    for (ParameterValidationResult result : exception.getParameterValidationResults()) {
      String field = result.getMethodParameter().getParameterName();
      for (MessageSourceResolvable error : result.getResolvableErrors()) {
        fieldErrors.add(new ApiFieldError(
            field == null ? "request" : field,
            safeReason(error.getDefaultMessage())
        ));
      }
    }
    return new ResponseEntity<>(validationProblem(fieldErrors), headers, status);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    String field = exception instanceof MethodArgumentTypeMismatchException methodArgumentException
        ? methodArgumentException.getName()
        : "request";
    ApiProblemDetail problemDetail = validationProblem(List.of(new ApiFieldError(
        field,
        "요청 값의 형식이 올바르지 않다."
    )));
    return new ResponseEntity<>(problemDetail, headers, status);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    ApiProblemDetail problemDetail = ApiProblemDetail.of(
        HttpStatus.BAD_REQUEST,
        "MALFORMED_REQUEST",
        "JSON 요청 본문을 읽을 수 없다."
    );
    return new ResponseEntity<>(problemDetail, headers, status);
  }

  @ExceptionHandler(Exception.class)
  ApiProblemDetail handleUnexpected(Exception exception) {
    log.error("예상하지 못한 API 오류가 발생했다.", exception);
    return ApiProblemDetail.of(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "예상하지 못한 서버 오류가 발생했다."
    );
  }

  private ApiProblemDetail validationProblem(List<ApiFieldError> fieldErrors) {
    return ApiProblemDetail.of(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "요청 값이 올바르지 않다.",
        sortedDistinct(fieldErrors)
    );
  }

  private ApiFieldError toApiFieldError(FieldError fieldError) {
    return new ApiFieldError(
        fieldError.getField(),
        safeReason(fieldError.getDefaultMessage())
    );
  }

  private List<ApiFieldError> sortedDistinct(List<ApiFieldError> fieldErrors) {
    return fieldErrors.stream()
        .distinct()
        .sorted(Comparator.comparing(ApiFieldError::field)
            .thenComparing(ApiFieldError::reason))
        .toList();
  }

  private String safeReason(String reason) {
    return reason == null ? "요청 값이 올바르지 않다." : reason;
  }
}
