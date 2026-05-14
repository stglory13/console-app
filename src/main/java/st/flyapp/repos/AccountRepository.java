package st.flyapp.repos;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import st.flyapp.model.Account;

/**
 * Spring Data JPA repository nad entitou Account.
 * Okrem štandardných CRUD metód poskytuje vyhľadávanie podľa verejného GUID-u.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByGuid(UUID guid);
}
