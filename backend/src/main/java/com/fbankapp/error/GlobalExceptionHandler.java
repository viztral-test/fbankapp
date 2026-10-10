package com.fbankapp.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (ex.getStatus() == HttpStatus.UNAUTHORIZED) {
            headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return respond(ex, ex.getStatus(), ex.getMessage(), null, headers, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return respond(ex, HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", null,
                new HttpHeaders(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        // Details stay in the server log; the client only gets a generic message
        log.error("Unhandled exception", ex);
        return respond(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", null, new HttpHeaders(),
                request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return respond(ex, status, "Validation failed", fieldErrors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        return respond(ex, status, "Malformed or unreadable JSON request body", null, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        Object responseBody = body != null ? body : body(status, status.getReasonPhrase(), null, request);
        return super.handleExceptionInternal(ex, responseBody, headers, status, request);
    }

    private ResponseEntity<Object> respond(Exception ex, HttpStatus status, String message,
            Map<String, String> fieldErrors, HttpHeaders headers, WebRequest request) {
        return handleExceptionInternal(ex, body(status, message, fieldErrors, request), headers, status, request);
    }

    private static ErrorResponse body(HttpStatus status, String message, Map<String, String> fieldErrors,
            WebRequest request) {
        String path = request instanceof ServletWebRequest
                ? ((ServletWebRequest) request).getRequest().getRequestURI()
                : null;
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path,
                fieldErrors);
    }
}
