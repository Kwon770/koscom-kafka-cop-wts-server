package com.koscom.kafkacop.web.exception;

import com.koscom.kafkacop.web.config.ErrorCode;
import com.koscom.kafkacop.web.controller.dto.ErrorType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DomainException extends RuntimeException {
    private final ErrorCode errorCode;
    private final ErrorType errorType;

    public DomainException(ErrorCode errorCode) {
        this(errorCode, ErrorType.NONE);
    }
}
