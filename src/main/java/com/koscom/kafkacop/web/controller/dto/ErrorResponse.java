package com.koscom.kafkacop.web.controller.dto;

import com.koscom.kafkacop.web.config.ErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "공통 에러 응답 DTO")
public record ErrorResponse(
        @Schema(description = "에러 코드", example = "AU0001")
        String code,

        @Schema(description = "에러 메시지", example = "인증에 실패했습니다.")
        String message,

        @Schema(description = "에러 종류 (에러 메시지를 노출할 수단을 제안, 기본값 NONE이며 디버그용)", example = "ALERT")
        ErrorType type
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, ErrorType.NONE);
    }

    public static ErrorResponse of(String code, String message, String type) {
        return new ErrorResponse(code, message, ErrorType.valueOf(type));
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), ErrorType.NONE);
    }

    public static ErrorResponse of(ErrorCode errorCode, ErrorType errorType) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errorType);
    }
}
