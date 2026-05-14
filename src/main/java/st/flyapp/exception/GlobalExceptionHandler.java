package st.flyapp.exception;

import static st.flyapp.logging.LogsCategorization.TECHNICAL_MARKER;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centrálny handler výnimiek pre celú REST vrstvu.
 * Mapuje doménové aj framework výnimky na HTTP odpovede s konzistentným telom a logovaním.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Nenájdená entita v DB — mapuje sa na HTTP 404 Not Found.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException ex) {
        log.warn(TECHNICAL_MARKER, "Handled NotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Porušenie biznis pravidla (napr. prekročený limit prečerpania) — HTTP 400 Bad Request
     * s message-om určeným pre frontend.
     */
    @ExceptionHandler(BiznisValidationFailedException.class)
    public ResponseEntity<String> handleValidation(BiznisValidationFailedException ex) {
        log.warn(TECHNICAL_MARKER, "Handled BiznisValidationFailedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Zlyhanie Bean Validation (@Valid na request body) — HTTP 400 s konkatenovaným zoznamom
     * field error-ov v tvare „field: message; ...".
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleBeanValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn(TECHNICAL_MARKER, "Handled bean validation failure: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    /**
     * Autentifikovaný používateľ bez potrebnej role (Spring Security @PreAuthorize) — HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
        log.warn(TECHNICAL_MARKER, "Handled AccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    /**
     * Optimistic locking konflikt (verzia entity sa medzi-čítaním a zápisom zmenila) — HTTP 409 Conflict.
     * Klient by mal request zopakovať po načítaní aktuálneho stavu.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn(TECHNICAL_MARKER, "Handled optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Concurrent update conflict: " + ex.getMessage());
    }

    /**
     * Posledná záchranná sieť pre neočakávané výnimky — HTTP 500 Internal Server Error.
     * Loguje s ERROR úrovňou aj stack-trace, aby šlo dohľadať príčinu.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        log.error(TECHNICAL_MARKER, "Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred: " + ex.getMessage());
    }
}
