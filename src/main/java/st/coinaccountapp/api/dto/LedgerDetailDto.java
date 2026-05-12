package st.coinaccountapp.api.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants
@Builder
@Schema(description = "Ledger detail DTO")
public class LedgerDetailDto {

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "Ledger ID")
    Long ledgerId;

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "From account UUID identifier")
    UUID fromAccountGuid;

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "To account UUID identifier")
    UUID toAccountGuid;

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "amount")
    BigDecimal amount;

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "balance after")
    BigDecimal balanceAfter;

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "Transaction time")
    LocalDateTime time;

    @Schema(description = "Transaction description")
    String description;
}
