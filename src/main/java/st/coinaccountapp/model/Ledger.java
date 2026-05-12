package st.coinaccountapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.AbstractPersistable;

/**
 * JPA entita zaznamenávajúca jednu transakciu medzi dvomi účtami.
 * Drží sumu, zostatok zdrojového účtu po, čas a popis. Audituje sa cez Hibernate Envers.
 */
@Entity(name = "ledger")
@Audited
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@ToString(of = {"description"})
public class Ledger extends AbstractPersistable<Long> {
    public Ledger(
            @NonNull Account fromAccount,
            @NonNull Account toAccount,
            @NonNull BigDecimal amount,
            @NonNull BigDecimal balanceAfter,
            String description) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.time = LocalDateTime.now();
    }

    @NonNull
    @ManyToOne
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    @NonNull
    @ManyToOne
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @NonNull
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NonNull
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @NonNull
    @Column(name = "time")
    private LocalDateTime time;

    @Column(name = "description")
    private String description;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
