package st.coinaccountapp.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

import st.coinaccountapp.exception.BiznisValidationFailedException;
import st.coinaccountapp.api.dto.CreateTransactionDto;
import st.coinaccountapp.api.dto.LedgerDetailDto;
import st.coinaccountapp.model.Account;
import st.coinaccountapp.model.Ledger;
import st.coinaccountapp.repos.LedgerRepository;


import static st.coinaccountapp.logging.LogsCategorization.BUSINESS_MARKER;

@Service
@Transactional
@Slf4j
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final AccountService accountService;

    public LedgerService(LedgerRepository ledgerRepository,
                         AccountService accountService) {
        this.ledgerRepository = ledgerRepository;
        this.accountService = accountService;
    }

    public LedgerDetailDto createTransaction(@NonNull CreateTransactionDto createTransactionRequest) {
        log.debug(BUSINESS_MARKER, "START transaction, request: {}", createTransactionRequest);

        Account fromAccount = accountService.getByGuid(createTransactionRequest.getFromAccountGuid());
        Account toAccount = accountService.getByGuid(createTransactionRequest.getToAccountGuid());
        BigDecimal fromAccountBalanceAfter = fromAccount.getCurrentBalance().subtract(createTransactionRequest.getAmount());
        BigDecimal toAccountBalanceAfter = toAccount.getCurrentBalance().add(createTransactionRequest.getAmount());

        if (createTransactionRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BiznisValidationFailedException("Transaction amount: %s is not valid, account guid: %s"
                    .formatted(createTransactionRequest.getAmount(), fromAccount.getGuid()));
        }

        if (fromAccountBalanceAfter.compareTo(fromAccount.getMaximalOverdraft().negate()) < 0) {
            throw new BiznisValidationFailedException("Transaction amount: %s exceeds maximalOverdraft: %s limit, account guid: %s"
                    .formatted(createTransactionRequest.getAmount(), fromAccount.getMaximalOverdraft(), fromAccount.getGuid()));
        }

        Ledger ledger = new Ledger(fromAccount, toAccount, createTransactionRequest.getAmount(), fromAccountBalanceAfter, createTransactionRequest.getDescription());
        ledger.setDescription(createTransactionRequest.getDescription());
        ledger = ledgerRepository.saveAndFlush(ledger);

        fromAccount.setCurrentBalance(fromAccountBalanceAfter);
        accountService.save(fromAccount);

        toAccount.setCurrentBalance(toAccountBalanceAfter);
        accountService.save(toAccount);

        LedgerDetailDto ledgerDetailDto = toDto(ledger);
        log.debug(BUSINESS_MARKER, "END transaction, Ledger detail: {}", ledgerDetailDto);
        return ledgerDetailDto;
    }

    private LedgerDetailDto toDto(@NonNull Ledger ledger) {
        return LedgerDetailDto.builder()
                .ledgerId(Objects.requireNonNull(ledger.getId()))
                .fromAccountGuid(Objects.requireNonNull(ledger.getFromAccount().getGuid()))
                .toAccountGuid(Objects.requireNonNull(ledger.getToAccount().getGuid()))
                .amount(ledger.getAmount())
                .balanceAfter(ledger.getBalanceAfter())
                .time(ledger.getTime())
                .description(ledger.getDescription())
                .build();
    }

}
