package st.coinaccountapp.api.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Schema(description = "New transaction request")
public class CreateTransactionDto {

    @NonNull
    @NotNull
    @Schema(requiredMode = REQUIRED, description = "from account GUID")
    private UUID fromAccountGuid;

    @NonNull
    @NotNull
    @Schema(requiredMode = REQUIRED, description = "to account GUID")
    private UUID toAccountGuid;

    @NonNull
    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "amount must be greater than 0")
    @Schema(requiredMode = REQUIRED, description = "amount (must be > 0)")
    private BigDecimal amount;

    @Size(max = 255, message = "description must be at most 255 characters")
    @Schema(description = "Description of the transaction")
    private String description;
}
