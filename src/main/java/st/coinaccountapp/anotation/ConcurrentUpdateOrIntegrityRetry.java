package st.coinaccountapp.anotation;

import jakarta.persistence.OptimisticLockException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * Annotation that ensures the method is executed within a retry block, where retries are applied
 * only for exceptions indicating a concurrent overwrite of a database entity or data integrity issues
 * (e.g., a duplicate unique key):<br>
 * {@link OptimisticLockException}<br>
 * {@link OptimisticEntityLockException}<br>
 * {@link ConcurrencyFailureException}<br>
 * {@link StaleObjectStateException}<br>
 *
 * Data integrity exceptions: These may occur during concurrent processing, and we want to handle them in the same way:<br>
 * {@link DataIntegrityViolationException}<br>
 * {@link ConstraintViolationException}<br>
 *
 * <p>The goal is to retry the DAO operation with a short delay.</p>
 *
 * <p>Note: This should be used on a method that is not already executed within an open database transaction.</p>
 */
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Retryable(retryFor = {OptimisticLockException.class, OptimisticEntityLockException.class,
                        ConcurrencyFailureException.class, StaleObjectStateException.class,
                        DataIntegrityViolationException.class,
                        ConstraintViolationException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 3.0))
public @interface ConcurrentUpdateOrIntegrityRetry {

}
