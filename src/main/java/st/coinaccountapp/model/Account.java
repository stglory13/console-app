package st.coinaccountapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.AbstractPersistable;

/**
 * JPA entita reprezentujúca účet — drží zostatok, limit prečerpania a verejný GUID.
 * Audituje sa cez Hibernate Envers (tabuľka account_aud), konkurenčné zmeny chráni optimistic locking.
 */
@Entity(name = "account")
@Audited
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@ToString(of = {"name"})
public class Account extends AbstractPersistable<Long> {

    /**
     * Unique identifier of account
     */
    @NonNull
    @Column(name = "guid", nullable = false, updatable = false, unique = true)
    private UUID guid;

    @NonNull
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Maximum allowed overdraft amount (if the account goes below zero, how far we can go)
     * de: genehmigter Überziehungskredit
     * sk: Povolené prečerpanie
     */
    @NonNull
    @Column(name = "maximal_overdraft", nullable = false, precision = 19, scale = 4)
    private BigDecimal maximalOverdraft;

    @NonNull
    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Version
    @Column(name = "version")
    @SuppressWarnings("unused")
    private long version;
}
