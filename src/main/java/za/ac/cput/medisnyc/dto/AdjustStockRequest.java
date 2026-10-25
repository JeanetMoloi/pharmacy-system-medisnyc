package za.ac.cput.medisnyc.dto;

import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(
        @NotNull(message = "Stock adjustment delta is required") Integer delta,
        @NotNull(message = "Adjustment reason is required") AdjustmentReason reason
) {
    public enum AdjustmentReason {
        RECEIVED_CORRECTION,
        DAMAGED,
        EXPIRED_WRITE_OFF,
        STOCK_TAKE,
        OTHER
    }
}
