package st.coinaccountapp.repos;

import st.coinaccountapp.model.Account;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import st.coinaccountapp.model.Ledger;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    List<Ledger> findAllByFromAccountOrToAccount(Account fromAccount, Account toAccount);

}
