package st.flyapp.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import st.flyapp.model.Ledger;

/**
 * Spring Data JPA repository nad entitou Ledger.
 * Poskytuje štandardné CRUD operácie pre záznamy transakcií.
 */
public interface LedgerRepository extends JpaRepository<Ledger, Long> {}
