package com.example.expenseclaim.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Catches when an entity/ID does not exist.
     * Returns HTTP 400 with a clear, descriptive error message instead of 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        String customMsg = "Invalid input: " + ex.getMessage() + ". Please check the ID and enter a valid existing record.";
        return buildResponse(HttpStatus.BAD_REQUEST, "Resource Not Found", customMsg);
    }

    /**
     * Catches business rule violations (policy limits, override requirements, unapproved finance payments).
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Business Rule Violation", ex.getMessage());
    }

    /**
     * Catches invalid workflow state transitions (e.g. paying already paid claim, already rejected).
     */
    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidState(InvalidStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid State Transition", ex.getMessage());
    }

    /**
     * Catches Bean Validation errors (@NotBlank, @Positive, @Email, etc.) on request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("message", "Input validation failed. Please check the 'errors' map for details.");
        body.put("errors", fieldErrors);
        body.put("timestamp", LocalDateTime.now().format(FORMATTER));
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Catches invalid parameter data types (e.g. entering 'abc' instead of a numeric ID in Swagger).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String enteredValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid number";

        String message = String.format("Invalid input for '%s': received '%s', but expected a %s. Please enter a valid number.",
                paramName, enteredValue, requiredType);
        return buildResponse(HttpStatus.BAD_REQUEST, "Type Mismatch Error", message);
    }

    /**
     * Catches malformed JSON payloads, wrong category enum values, or invalid date formats in Swagger request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String detailedMsg = "Malformed or unreadable JSON request body.";
        String exMsg = ex.getMessage() != null ? ex.getMessage() : "";

        if (exMsg.contains("ExpenseCategory")) {
            detailedMsg = "Invalid expense category entered. Allowed categories are strictly: TRAVEL, FOOD, HOTEL, FUEL.";
        } else if (exMsg.contains("LocalDate") || exMsg.contains("DateTimeParseException")) {
            detailedMsg = "Invalid date format entered. Expected date format is YYYY-MM-DD (e.g. 2026-09-28).";
        } else if (exMsg.contains("Cannot deserialize")) {
            detailedMsg = "Invalid field value or type mismatch in request JSON. Please check all field types.";
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "JSON Format Error", detailedMsg);
    }

    /**
     * Catches when an unsupported HTTP method is used on an endpoint in Swagger or cURL.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = String.format("HTTP method '%s' is not supported for this URL. Supported methods are: %s.",
                ex.getMethod(), java.util.Arrays.toString(ex.getSupportedMethods()));
        return buildResponse(HttpStatus.BAD_REQUEST, "Method Not Supported", message);
    }

    /**
     * Catches when an invalid or wrong URL path is called instead of returning a blank 404 page.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        String message = String.format("The requested path '/%s' does not exist. Please check the URL and method in Swagger UI.",
                ex.getResourcePath());
        return buildResponse(HttpStatus.BAD_REQUEST, "Endpoint Not Found", message);
    }

    /**
     * Catches illegal arguments.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Illegal Argument", ex.getMessage());
    }

    /**
     * General fallback exception handler returning clean JSON error response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", "Error processing request: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().format(FORMATTER));
        return ResponseEntity.status(status).body(body);
    }
}
