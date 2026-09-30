package io.github.krapnuyij.logiops.common.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "요청 필드 검증 오류")
public record ApiFieldError(
    @Schema(description = "오류가 발생한 필드 경로", example = "items[0].quantity")
    String field,
    @Schema(description = "안전한 검증 실패 사유", example = "주문 수량은 양수여야 한다.")
    String reason
) {
}
