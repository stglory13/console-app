package st.coinaccountapp.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;


import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@Schema(description = "New transaction request")
public class CreateTransactionDto {

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "from account GUID")
    private UUID fromAccountGuid;

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "to account GUID")
    private UUID toAccountGuid;

    @NonNull
    @Schema(requiredMode = REQUIRED, description = "amount")
    private BigDecimal amount;

    @Schema(description = "Description of the transaction")
    private String description;
}
