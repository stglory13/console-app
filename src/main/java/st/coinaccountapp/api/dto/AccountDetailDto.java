package st.coinaccountapp.api.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants
@Schema(description = "Account detail DTO")
public class AccountDetailDto {

    @JsonCreator
    public AccountDetailDto(
            @JsonProperty("guid") UUID guid,
            @JsonProperty("name") String name,
            @JsonProperty("maximalOverdraft") BigDecimal maximalOverdraft,
            @JsonProperty("currentBalance") BigDecimal currentBalance) {
        this.guid = guid;
        this.name = name;
        this.maximalOverdraft = maximalOverdraft;
        this.currentBalance = currentBalance;
    }

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "Account unique public identifier")
    private UUID guid;

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "Account holder's name")
    private String name;

    @Schema(description = "Maximum overdraft allowed in the account (if the account goes below zero)")
    private BigDecimal maximalOverdraft;

    @Schema(description = "Current balance in the account")
    private BigDecimal currentBalance;
}
