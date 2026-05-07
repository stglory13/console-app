package st.coinaccountapp;

import st.coinaccountapp.api.dto.CreateTransactionDto;
import st.coinaccountapp.api.dto.LedgerDetailDto;
import st.coinaccountapp.config.ApiPaths;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import st.coinaccountapp.api.dto.AccountDetailDto;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
@SpringBootTest(classes = CoinAccountApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CoinAccountApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    int randomServerPort;

    String getAccountUrl;
    String createTransactionUrl;
    UUID account1Guid;
    UUID account2Guid;
    UUID account3Guid;
    String nonValidAccountGuid;
    UUID nonExistentAccountGuid;

    @BeforeEach
    public void setUp() throws Exception {
        String baseUrl = "http://localhost:"+randomServerPort;
        getAccountUrl = baseUrl + ApiPaths.ACCOUNT_GET.replace("{guid}", "");
        createTransactionUrl = baseUrl + ApiPaths.TRANSACTION_POST;
        account1Guid = UUID.fromString("8d9d35e2-15b3-4fad-b853-f5731e9e19fa");
        account2Guid = UUID.fromString("d1e39c65-48c9-42ef-9c50-8dd5a072e510");
        account3Guid = UUID.fromString("6eb7e588-5d85-4285-8c64-3be32a70393b");
        nonValidAccountGuid = "not-a-valid-uuid";
        nonExistentAccountGuid = UUID.randomUUID();
    }

    /**
     * Test suite for the coin account app services functionality
     * Test the retrieval of account details
     * Test the successful creation of a new transaction
     * Test is in one suite to cover all edge cases and set up proper conditions.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testCoinAccountApp() throws URISyntaxException {
        // Test the retrieval of account details.
        ResponseEntity<AccountDetailDto> accountResponse1 = restTemplate.getForEntity(new URI(getAccountUrl + account1Guid), AccountDetailDto.class);

        assertThat(accountResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);
        AccountDetailDto accountDetail1 = accountResponse1.getBody();
        assertThat(accountDetail1)
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account1Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 1")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("1000.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("913.1300"));

        ResponseEntity<AccountDetailDto> accountResponse2 = restTemplate.getForEntity(new URI(getAccountUrl + account2Guid), AccountDetailDto.class);

        assertThat(accountResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        AccountDetailDto accountDetail2 = accountResponse2.getBody();
        assertThat(accountDetail2)
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account2Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 2")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("500.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("1513.5000"));

        ResponseEntity<AccountDetailDto> accountResponse3 = restTemplate.getForEntity(new URI(getAccountUrl + account3Guid), AccountDetailDto.class);

        assertThat(accountResponse3.getStatusCode()).isEqualTo(HttpStatus.OK);
        AccountDetailDto accountDetail3 = accountResponse3.getBody();
        assertThat(accountDetail3)
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account3Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 3")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("200.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("700.0000"));

        // Test the retrieval of account details - data not found for non-existent account
        ResponseEntity<String> accountResponseNotFound = restTemplate.getForEntity(new URI(getAccountUrl + nonExistentAccountGuid), String.class);

        assertThat(accountResponseNotFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(accountResponseNotFound.getBody()).contains("Data not found: " + nonExistentAccountGuid);

        // Test the retrieval of account details - invalid UUID format
        ResponseEntity<String> accountResponseInvalid = restTemplate.getForEntity(new URI(getAccountUrl + nonValidAccountGuid), String.class);

        assertThat(accountResponseInvalid.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(accountResponseInvalid.getBody()).contains("An unexpected error occurred: Failed to convert value of type 'java.lang.String'");

        // Test Ledger transactions - create transaction from account 3 to account 2
        CreateTransactionDto createTransactionRequest = new CreateTransactionDto(account3Guid, account2Guid, new BigDecimal("100.0000"));
        createTransactionRequest.setDescription("Test transaction account 3 to account 2");
        ResponseEntity<LedgerDetailDto> createTransactionResponse = restTemplate.postForEntity(new URI(createTransactionUrl), createTransactionRequest, LedgerDetailDto.class);

        assertThat(createTransactionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LedgerDetailDto ledgerDetail1Dto = createTransactionResponse.getBody();
        assertThat(ledgerDetail1Dto)
                .isNotNull()
                .hasFieldOrPropertyWithValue(LedgerDetailDto.Fields.fromAccountGuid, account3Guid)
                .hasFieldOrPropertyWithValue(LedgerDetailDto.Fields.toAccountGuid, account2Guid)
                .hasFieldOrPropertyWithValue(LedgerDetailDto.Fields.amount, new BigDecimal("100.0000"))
                .hasFieldOrPropertyWithValue(LedgerDetailDto.Fields.description, "Test transaction account 3 to account 2");

        assertThat(ledgerDetail1Dto.getTime()).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now());
        assertThat(ledgerDetail1Dto.getLedgerId()).isNotNull();

        accountResponse3 = restTemplate.getForEntity(new URI(getAccountUrl + account3Guid), AccountDetailDto.class);
        assertThat(accountResponse3.getStatusCode()).isEqualTo(HttpStatus.OK);
        accountDetail3 = accountResponse3.getBody();
        assertThat(accountDetail3)
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account3Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 3")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("200.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("600.0000")); // 700 - 100 = 600

        accountResponse2 = restTemplate.getForEntity(new URI(getAccountUrl + account2Guid), AccountDetailDto.class);
        assertThat(accountResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        accountDetail2 = accountResponse2.getBody();
        assertThat(accountDetail2)
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account2Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 2")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("500.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("1613.5000")); // 1513.50 + 100 = 1613.50

        // Test the successful creation of a new transaction - Transaction amount: 1914.00 exceeds maximalOverdraft for account 1
        CreateTransactionDto createTransactionRequestExceedMaximalOverdraft = new CreateTransactionDto(account1Guid, account2Guid, new BigDecimal("1914.0000"));
        createTransactionRequestExceedMaximalOverdraft.setDescription("Test transaction account 1 to account 2 exceeding maximal overdraft");
        ResponseEntity<String> createTransactionResponseExceedMaximalOverdraft = restTemplate.postForEntity(new URI(createTransactionUrl),
                createTransactionRequestExceedMaximalOverdraft, String.class);

        assertThat(createTransactionResponseExceedMaximalOverdraft.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(createTransactionResponseExceedMaximalOverdraft.getBody())
                .contains("Transaction amount: 1914.0000 exceeds maximalOverdraft: 1000.0000 limit, account guid: " + account1Guid);

        // Test transactions - Transaction amount: 0.00 is not valid
        CreateTransactionDto createTransactionRequestZeroAmount = new CreateTransactionDto(account1Guid, account2Guid, new BigDecimal("0.0000"));
        createTransactionRequestZeroAmount.setDescription("Test transaction account 1 to account 2 with zero amount");
        ResponseEntity<String> createTransactionResponseZeroAmount = restTemplate.postForEntity(new URI(createTransactionUrl),
                createTransactionRequestZeroAmount, String.class);

        assertThat(createTransactionResponseZeroAmount.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(createTransactionResponseZeroAmount.getBody()).contains("Transaction amount: 0.0000 is not valid, account guid: " + account1Guid);

        // Test transactions - Transaction amount: -500.00 is not valid
        CreateTransactionDto createTransactionRequestMinusAmount = new CreateTransactionDto(account1Guid, account2Guid, new BigDecimal("-500.0000"));
        createTransactionRequestZeroAmount.setDescription("Test transaction account 1 to account 2 with -500 amount");
        ResponseEntity<String> createTransactionResponseMinusAmount = restTemplate.postForEntity(new URI(createTransactionUrl),
                createTransactionRequestMinusAmount, String.class);

        assertThat(createTransactionResponseMinusAmount.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(createTransactionResponseMinusAmount.getBody()).contains("Transaction amount: -500.0000 is not valid, account guid: " + account1Guid);

        // Test Ledger transactions - Transaction amount: negative or zero value is not valid
        CreateTransactionDto createTransactionRequestNegativeAmount = new CreateTransactionDto(account1Guid, account2Guid, new BigDecimal("-100.0000"));
        createTransactionRequestNegativeAmount.setDescription("Test transaction account 1 to account 2 with negative amount");
        ResponseEntity<String> createTransactionResponseNegativeAmount = restTemplate.postForEntity(new URI(createTransactionUrl), createTransactionRequestNegativeAmount, String.class);
        assertThat(createTransactionResponseNegativeAmount.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(createTransactionResponseNegativeAmount.getBody()).contains("Transaction amount: -100.0000 is not valid, account guid: " + account1Guid);

        // check if account 1 and account 2 was not changed after the transaction failed
        accountResponse1 = restTemplate.getForEntity(new URI(getAccountUrl + account1Guid), AccountDetailDto.class);
        assertThat(accountResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);
        accountDetail1 = accountResponse1.getBody();
        assertThat(accountDetail1)
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account1Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 1")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("1000.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("913.1300"));

        accountResponse2 = restTemplate.getForEntity(new URI(getAccountUrl + account2Guid), AccountDetailDto.class);
        assertThat(accountResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        accountDetail2 = accountResponse2.getBody();
        assertThat(accountDetail2)
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account2Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 2")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("500.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("1613.5000"));

        // End of test suite
    }

}
