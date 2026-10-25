package za.ac.cput.medisnyc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReceiveStockRequest(
        @NotBlank(message = "Medication ID is required") String medicationId,
        @NotNull(message = "Stock level is required")
        @Min(value = 0, message = "Stock level must be zero or greater") Integer stockLevel,
        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future") LocalDate expiryDate,
        @NotBlank(message = "Batch number is required") String batchNumber,
        String supplier,
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price must be zero or greater") BigDecimal unitPrice,
        LocalDate receivedDate,
        @NotNull(message = "Reorder level is required")
        @Min(value = 0, message = "Reorder level must be zero or greater") Integer reorderLevel
) {
}
