package com.koscom.kafkacop.web.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    /**
     * Common Errors
     */
    UNKNOWN_SERVER_ERROR("CM0001", "Unknown Server Error"),
    INVALID_INPUT_VALUE("CM0002", "Invalid Input Value"),
    METHOD_NOT_ALLOWED("CM0003", "Method Not Allowed"),
    ACCESS_DENIED("CM0004", "Access Denied");

    private final String code;
    private final String message;
}
