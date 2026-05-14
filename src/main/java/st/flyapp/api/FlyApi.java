package st.flyapp.api;

import static st.flyapp.logging.LogsCategorization.PAYLOAD_MARKER;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import st.flyapp.api.dto.AccountDetailDto;
import st.flyapp.api.dto.CreateTransactionDto;
import st.flyapp.api.dto.LedgerDetailDto;
import st.flyapp.config.ApiPaths;
import st.flyapp.service.AccountService;
import st.flyapp.service.LedgerService;

/**
 * REST kontrolér pre operácie nad účtami a transakciami.
 * Vystavuje endpointy pod /v1/account, autorizácia cez Bearer JWT a metódové @PreAuthorize.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class FlyApi {

    private final AccountService accountService;
    private final LedgerService ledgerService;

    @GetMapping(ApiPaths.ACCOUNT_GET)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get account details by guid", security = @SecurityRequirement(name = "bearerAuth"))
    public AccountDetailDto getAccount(@PathVariable("guid") UUID guid) {
        log.debug(PAYLOAD_MARKER, "REQ GET account, guid: {}", guid);
        AccountDetailDto response = accountService.getAccountDetail(guid);
        log.debug(PAYLOAD_MARKER, "RES GET account, body: {}", response);
        return response;
    }

    @PostMapping(ApiPaths.TRANSACTION_POST)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Perform a transaction between two accounts",
            security = @SecurityRequirement(name = "bearerAuth"))
    public LedgerDetailDto createTransaction(@RequestBody @Valid CreateTransactionDto request) {
        log.debug(PAYLOAD_MARKER, "REQ POST transaction, body: {}", request);
        LedgerDetailDto response = ledgerService.createTransaction(request);
        log.debug(PAYLOAD_MARKER, "RES POST transaction, body: {}", response);
        return response;
    }
}
