package st.coinaccountapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import st.coinaccountapp.api.dto.AccountDetailDto;
import st.coinaccountapp.api.dto.CreateTransactionDto;
import st.coinaccountapp.api.dto.LedgerDetailDto;
import st.coinaccountapp.config.ApiPaths;
import st.coinaccountapp.model.Account;
import st.coinaccountapp.repos.AccountRepository;
import st.coinaccountapp.testsupport.IntegrationTestSecurityConfig;

@Testcontainers
@ActiveProfiles("localdev")
@SpringBootTest(classes = CoinAccountApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTestSecurityConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoinAccountApplicationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int randomServerPort;

    private String getAccountUrl;
    private String createTransactionUrl;
    private UUID account1Guid;
    private UUID account2Guid;
    private UUID account3Guid;
    private String nonValidAccountGuid;
    private UUID nonExistentAccountGuid;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().getInterceptors().clear();
        restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
            request.getHeaders().setBearerAuth("test-token");
            return execution.execute(request, body);
        });

        String baseUrl = "http://localhost:" + randomServerPort;
        getAccountUrl = baseUrl + ApiPaths.ACCOUNT_GET.replace("{guid}", "");
        createTransactionUrl = baseUrl + ApiPaths.TRANSACTION_POST;

        account1Guid = UUID.fromString("8d9d35e2-15b3-4fad-b853-f5731e9e19fa");
        account2Guid = UUID.fromString("d1e39c65-48c9-42ef-9c50-8dd5a072e510");
        account3Guid = UUID.fromString("6eb7e588-5d85-4285-8c64-3be32a70393b");
        nonValidAccountGuid = "not-a-valid-uuid";
        nonExistentAccountGuid = UUID.randomUUID();
    }

    @Test
    @Order(1)
    void getAccount_returnsCorrectDetail_forSeedAccount1() throws URISyntaxException {
        ResponseEntity<AccountDetailDto> response =
                restTemplate.getForEntity(new URI(getAccountUrl + account1Guid), AccountDetailDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account1Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 1")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("1000.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("913.1300"));
    }

    @Test
    @Order(2)
    void getAccount_returnsCorrectDetail_forSeedAccount2() throws URISyntaxException {
        ResponseEntity<AccountDetailDto> response =
                restTemplate.getForEntity(new URI(getAccountUrl + account2Guid), AccountDetailDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account2Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 2")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("500.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("1513.5000"));
    }

    @Test
    @Order(3)
    void getAccount_returnsCorrectDetail_forSeedAccount3() throws URISyntaxException {
        ResponseEntity<AccountDetailDto> response =
                restTemplate.getForEntity(new URI(getAccountUrl + account3Guid), AccountDetailDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.guid, account3Guid)
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.name, "TestAccount No 3")
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.maximalOverdraft, new BigDecimal("200.0000"))
                .hasFieldOrPropertyWithValue(AccountDetailDto.Fields.currentBalance, new BigDecimal("700.0000"));
    }

    @Test
    @Order(4)
    void getAccount_notFound_whenGuidDoesNotExist() throws URISyntaxException {
        ResponseEntity<String> response =
                restTemplate.getForEntity(new URI(getAccountUrl + nonExistentAccountGuid), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Data not found: " + nonExistentAccountGuid);
    }

    @Test
    @Order(5)
    void getAccount_internalServerError_whenInvalidGuid() throws URISyntaxException {
        ResponseEntity<String> response =
                restTemplate.getForEntity(new URI(getAccountUrl + nonValidAccountGuid), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .contains("An unexpected error occurred: Failed to convert value of type 'java.lang.String'");
    }

    @Test
    @Order(10)
    void createTransaction_badRequest_whenExceedsMaximalOverdraft() throws URISyntaxException {
        CreateTransactionDto request =
                new CreateTransactionDto(account1Guid, account2Guid, new BigDecimal("1914.0000"));
        request.setDescription("Exceeding overdraft");

        ResponseEntity<String> response =
                restTemplate.postForEntity(new URI(createTransactionUrl), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .contains("Transaction amount: 1914.0000 exceeds maximalOverdraft: 1000.0000 limit, account guid: "
                        + account1Guid);
    }

    @Test
    @Order(11)
    void createTransaction_badRequest_whenAmountZero() throws URISyntaxException {
        CreateTransactionDto request = new CreateTransactionDto(account1Guid, account2Guid, new BigDecimal("0.0000"));
        request.setDescription("Zero amount");

        ResponseEntity<String> response =
                restTemplate.postForEntity(new URI(createTransactionUrl), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("amount", "must be greater than 0");
    }

    @Test
    @Order(12)
    void createTransaction_badRequest_whenAmountNegative() throws URISyntaxException {
        CreateTransactionDto request =
                new CreateTransactionDto(account1Guid, account2Guid, new BigDecimal("-100.0000"));
        request.setDescription("Negative amount");

        ResponseEntity<String> response =
                restTemplate.postForEntity(new URI(createTransactionUrl), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("amount", "must be greater than 0");
    }

    @Test
    @Order(50)
    void createTransaction_success_updatesBothAccountBalances() throws URISyntaxException {
        BigDecimal fromBalanceBefore = restTemplate
                .getForEntity(new URI(getAccountUrl + account3Guid), AccountDetailDto.class)
                .getBody()
                .getCurrentBalance();
        BigDecimal toBalanceBefore = restTemplate
                .getForEntity(new URI(getAccountUrl + account2Guid), AccountDetailDto.class)
                .getBody()
                .getCurrentBalance();

        CreateTransactionDto request = new CreateTransactionDto(account3Guid, account2Guid, new BigDecimal("100.0000"));
        request.setDescription("Test transaction account 3 to account 2");

        ResponseEntity<LedgerDetailDto> response =
                restTemplate.postForEntity(new URI(createTransactionUrl), request, LedgerDetailDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LedgerDetailDto ledger = response.getBody();
        assertThat(ledger)
                .isNotNull()
                .hasFieldOrPropertyWithValue(LedgerDetailDto.Fields.fromAccountGuid, account3Guid)
                .hasFieldOrPropertyWithValue(LedgerDetailDto.Fields.toAccountGuid, account2Guid)
                .hasFieldOrPropertyWithValue(LedgerDetailDto.Fields.amount, new BigDecimal("100.0000"))
                .hasFieldOrPropertyWithValue(
                        LedgerDetailDto.Fields.description, "Test transaction account 3 to account 2");
        assertThat(ledger.getTime()).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now());
        assertThat(ledger.getLedgerId()).isNotNull();

        BigDecimal fromBalanceAfter = restTemplate
                .getForEntity(new URI(getAccountUrl + account3Guid), AccountDetailDto.class)
                .getBody()
                .getCurrentBalance();
        BigDecimal toBalanceAfter = restTemplate
                .getForEntity(new URI(getAccountUrl + account2Guid), AccountDetailDto.class)
                .getBody()
                .getCurrentBalance();

        assertThat(fromBalanceAfter).isEqualByComparingTo(fromBalanceBefore.subtract(new BigDecimal("100.0000")));
        assertThat(toBalanceAfter).isEqualByComparingTo(toBalanceBefore.add(new BigDecimal("100.0000")));
    }

    /**
     * Optimisticky locking: ak nejaka ina transakcia medzitym aktualizovala riadok
     * (zvysila version), pokus o ulozenie stareho snapshotu musi skoncit konfliktom.
     */
    @Test
    @Order(99)
    void optimisticLocking_staleEntityUpdate_throwsConflict() {
        Account stale = accountRepository.findByGuid(account1Guid).orElseThrow();

        int updated = jdbcTemplate.update("UPDATE account SET version = version + 1 WHERE guid = ?", account1Guid);
        assertThat(updated).isEqualTo(1);

        stale.setName("StaleRename");

        assertThatThrownBy(() -> accountRepository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
