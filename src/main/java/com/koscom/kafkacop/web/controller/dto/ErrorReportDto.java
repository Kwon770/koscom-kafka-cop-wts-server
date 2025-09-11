package com.koscom.kafkacop.web.controller.dto;

public record ErrorReportDto(
        String message,
        String payload
) {
}
