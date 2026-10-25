package za.ac.cput.medisnyc.dto;

import java.time.LocalDate;

public record InventorySummary(
        String medicationId,
        String medicationName,
        String dosageForm,
        String strength,
        long totalStock,
        long batchCount,
        LocalDate earliestExpiry,
        String status,
        long expiredBatchCount,
        long expiringSoonBatchCount
) {
}
