package st.coinaccountapp.service;

import static st.coinaccountapp.logging.LogsCategorization.BUSINESS_MARKER;

import java.math.BigDecimal;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.coinaccountapp.api.dto.CreateTransactionDto;
import st.coinaccountapp.api.dto.LedgerDetailDto;
import st.coinaccountapp.api.mapper.LedgerMapper;
import st.coinaccountapp.exception.BiznisValidationFailedException;
import st.coinaccountapp.model.Account;
import st.coinaccountapp.model.Ledger;
import st.coinaccountapp.repos.LedgerRepository;

/**
 * Biznis logika nad finančnou knihou (ledger).
 * Vykonáva presun prostriedkov medzi dvomi účtami, validuje limit prečerpania
 * a zapisuje záznam o transakcii. Bežné štrukturálne validácie vstupu
 * (kladná suma, non-null polia) sú v {@link CreateTransactionDto} cez Bean Validation.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final AccountService accountService;
    private final LedgerMapper ledgerMapper;

    public LedgerDetailDto createTransaction(@NonNull CreateTransactionDto createTransactionRequest) {
        log.debug(BUSINESS_MARKER, "START transaction, request: {}", createTransactionRequest);

        Account fromAccount = accountService.getByGuid(createTransactionRequest.getFromAccountGuid());
        Account toAccount = accountService.getByGuid(createTransactionRequest.getToAccountGuid());
        BigDecimal fromAccountBalanceAfter =
                fromAccount.getCurrentBalance().subtract(createTransactionRequest.getAmount());
        BigDecimal toAccountBalanceAfter = toAccount.getCurrentBalance().add(createTransactionRequest.getAmount());

        if (fromAccountBalanceAfter.compareTo(fromAccount.getMaximalOverdraft().negate()) < 0) {
            throw new BiznisValidationFailedException(
                    "Transaction amount: %s exceeds maximalOverdraft: %s limit, account guid: %s"
                            .formatted(
                                    createTransactionRequest.getAmount(),
                                    fromAccount.getMaximalOverdraft(),
                                    fromAccount.getGuid()));
        }

        Ledger ledger = new Ledger(
                fromAccount,
                toAccount,
                createTransactionRequest.getAmount(),
                fromAccountBalanceAfter,
                createTransactionRequest.getDescription());
        ledger.setDescription(createTransactionRequest.getDescription());
        ledger = ledgerRepository.saveAndFlush(ledger);

        fromAccount.setCurrentBalance(fromAccountBalanceAfter);
        accountService.save(fromAccount);

        toAccount.setCurrentBalance(toAccountBalanceAfter);
        accountService.save(toAccount);

        LedgerDetailDto ledgerDetailDto = ledgerMapper.toDto(ledger);
        log.debug(BUSINESS_MARKER, "END transaction, Ledger detail: {}", ledgerDetailDto);
        return ledgerDetailDto;
    }
}
