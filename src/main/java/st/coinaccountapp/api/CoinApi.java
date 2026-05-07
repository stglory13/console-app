package st.coinaccountapp.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import st.coinaccountapp.anotation.ConcurrentUpdateOrIntegrityRetry;
import st.coinaccountapp.api.dto.AccountDetailDto;
import st.coinaccountapp.api.dto.CreateTransactionDto;
import st.coinaccountapp.api.dto.LedgerDetailDto;
import st.coinaccountapp.config.ApiPaths;
import st.coinaccountapp.service.AccountService;
import st.coinaccountapp.service.LedgerService;

@RestController
public class CoinApi {

    private final AccountService accountService;
    private final LedgerService ledgerService;

    public CoinApi(AccountService accountService,
                   LedgerService ledgerService) {
        this.accountService = accountService;
        this.ledgerService = ledgerService;
    }

    @GetMapping(ApiPaths.ACCOUNT_GET)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get account details by guid")
    public AccountDetailDto getAccount(@PathVariable("guid") UUID guid) {
        return accountService.getAccountDetail(guid);
    }

    @PostMapping(ApiPaths.TRANSACTION_POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ConcurrentUpdateOrIntegrityRetry
    @Operation(summary = "Perform a transaction between two accounts")
    public LedgerDetailDto createTransaction(@RequestBody @Valid CreateTransactionDto request) {
        return ledgerService.createTransaction(request);
    }
}
