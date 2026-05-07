package st.coinaccountapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity(name = "account")
@RequiredArgsConstructor @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter @Setter
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

}
