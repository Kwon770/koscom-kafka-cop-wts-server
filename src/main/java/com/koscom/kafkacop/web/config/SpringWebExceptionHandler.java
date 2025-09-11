package com.koscom.kafkacop.web.config;

import static org.springframework.http.HttpStatus.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.InvalidParameterException;
import java.util.Enumeration;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.koscom.kafkacop.web.controller.dto.ErrorReportDto;
import com.koscom.kafkacop.web.controller.dto.ErrorResponse;
import com.koscom.kafkacop.web.controller.dto.ErrorType;
import com.koscom.kafkacop.web.exception.DomainException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class SpringWebExceptionHandler {
    final ApplicationEventPublisher eventPublisher;

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomainException(HttpServletRequest req, DomainException exception) {
        if (exception.getErrorCode() == ErrorCode.UNKNOWN_SERVER_ERROR) {
            return handleUnhandledException(req, exception);
        }

        log.warn("[DomainException]", exception);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(exception.getErrorCode(), exception.getErrorType()));
    }

    @ExceptionHandler(value = {
            HttpMessageNotReadableException.class,
            InvalidParameterException.class,
            ServletRequestBindingException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
    })
    ResponseEntity<ErrorResponse> handleValidateException(Exception exception) {
        log.warn("[{}}]", exception.getClass().getSimpleName(), exception);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, ErrorType.NONE));
    }

    @ExceptionHandler(value = {
            HttpRequestMethodNotSupportedException.class,
    })
    ResponseEntity<ErrorResponse> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException exception) {
        log.warn("[HttpRequestMethodNotSupportedException]", exception);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, ErrorType.NONE));
    }

    @ExceptionHandler(IOException.class)
    ResponseEntity<ErrorResponse> handleClientCancelException(IOException exception) {

        if (exception.getMessage().contains("Broken pipe")) {
            log.warn("[IOException] Broken Pipe");
        } else {
            log.error("[IOException]", exception);
        }

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.UNKNOWN_SERVER_ERROR, ErrorType.NONE));
    }


    @ExceptionHandler(Throwable.class)
    ResponseEntity<ErrorResponse> handleUnhandledException(HttpServletRequest request, Throwable exception) {

        StringBuilder dump = dumpRequest(request).append("\n ").append(getStackTraceAsString(exception));
        log.error("[UnhandledException] {} \n", dump);

        eventPublisher.publishEvent(new ErrorReportDto(exception.getMessage(), dump.toString()));
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.UNKNOWN_SERVER_ERROR, ErrorType.ALERT));
    }

    private StringBuilder dumpRequest(HttpServletRequest request) {
        StringBuilder dump = new StringBuilder("HttpRequest Dump:")
                .append("\n  Remote Addr   : ").append(request.getRemoteAddr())
                .append("\n  Protocol      : ").append(request.getProtocol())
                .append("\n  Request Method: ").append(request.getMethod())
                .append("\n  Request URI   : ").append(request.getRequestURI())
                .append("\n  Query String  : ").append(request.getQueryString())
                .append("\n  Parameters    : ");

        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            dump.append("\n    ").append(name).append('=');
            String[] parameterValues = request.getParameterValues(name);
            for (String value : parameterValues) {
                dump.append(value);
            }
        }

        dump.append("\n  Headers       : ");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            dump.append("\n    ").append(name).append(":");
            Enumeration<String> headerValues = request.getHeaders(name);
            while (headerValues.hasMoreElements()) {
                dump.append(headerValues.nextElement());
            }
        }

        dump.append("\n  Body       : ");
        try {
            BufferedReader reader = request.getReader();
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append("\n");
            }
            dump.append("\n    ").append(body);
        }catch(Exception ex) {
            dump.append("\n    ").append("NOT_READABLE");
        }

        return dump;
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
