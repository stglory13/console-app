package st.coinaccountapp.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import st.coinaccountapp.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

        Optional<Account> findByGuid(UUID guid);

}
