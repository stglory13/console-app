package st.coinaccountapp.service;

import static st.coinaccountapp.logging.LogsCategorization.BUSINESS_MARKER;

import java.util.Objects;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.coinaccountapp.api.dto.AccountDetailDto;
import st.coinaccountapp.exception.NotFoundException;
import st.coinaccountapp.model.Account;
import st.coinaccountapp.repos.AccountRepository;

/**
 * Biznis logika nad účtami — načítanie podľa GUID-u, uloženie a vrátenie detailu pre REST vrstvu.
 * Operácie bežia v transakcii, optimistic locking zabezpečuje konzistentnosť pri konkurenčných zmenách.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountDetailDto getAccountDetail(@NonNull UUID guid) {
        log.debug(BUSINESS_MARKER, "Get detail of account with id: {}", guid);
        Account account = getByGuid(guid);
        AccountDetailDto dto = toDto(account);
        log.debug(BUSINESS_MARKER, "Detail of account: {}", dto);
        return dto;
    }

    public Account getByGuid(@NonNull UUID guid) {
        return accountRepository.findByGuid(guid).orElseThrow(() -> new NotFoundException(guid));
    }

    public void save(@NonNull Account account) {
        log.debug(
                BUSINESS_MARKER,
                "SAVE account with id: {}, name: {}, balance: {}",
                account.getId(),
                account.getName(),
                account.getCurrentBalance());
        accountRepository.saveAndFlush(account);
    }

    private AccountDetailDto toDto(@NonNull Account account) {
        return new AccountDetailDto(
                Objects.requireNonNull(account.getGuid()),
                account.getName(),
                account.getMaximalOverdraft(),
                account.getCurrentBalance());
    }
}
