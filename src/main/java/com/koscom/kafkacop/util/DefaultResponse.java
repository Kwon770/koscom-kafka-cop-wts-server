package com.koscom.kafkacop.util;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기본 응답")
public record DefaultResponse(
        @Schema(description = "성공 유무", example = "true")
        boolean success
) {
    public static DefaultResponse ok() {
        return new DefaultResponse(true);
    }

    public static DefaultResponse fail() {
        return new DefaultResponse(false);
    }
}
