package za.ac.cput.medisnyc.service;

import za.ac.cput.medisnyc.domain.Inventory;
import za.ac.cput.medisnyc.dto.AdjustStockRequest;
import za.ac.cput.medisnyc.dto.InventorySummary;
import za.ac.cput.medisnyc.dto.ReceiveStockRequest;
import za.ac.cput.medisnyc.exception.ResourceNotFoundException;
import za.ac.cput.medisnyc.repository.jpa.InventoryJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.MedicationJpaRepository;
import za.ac.cput.medisnyc.util.Helper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryJpaRepository inventoryRepository;
    private final MedicationJpaRepository medicationRepository;
    private final AuditLogService auditLogService;

    public InventoryService(InventoryJpaRepository inventoryRepository,
                            MedicationJpaRepository medicationRepository,
                            AuditLogService auditLogService) {
        this.inventoryRepository = inventoryRepository;
        this.medicationRepository = medicationRepository;
        this.auditLogService = auditLogService;
    }

    public List<Inventory> getAll() {
        return inventoryRepository.findAll();
    }

    public Inventory getById(String inventoryId) {
        return inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found: " + inventoryId));
    }

    public List<Inventory> getByMedication(String medicationId) {
        return inventoryRepository.findByMedicationId(medicationId);
    }

    @Transactional
    public Inventory receiveStock(ReceiveStockRequest request, String actingUsername) {
        if (!medicationRepository.existsById(request.medicationId())) {
            throw new IllegalArgumentException("Medication ID does not exist: " + request.medicationId());
        }
        if (request.expiryDate() == null || !request.expiryDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expiry date must be in the future");
        }
        if (request.stockLevel() < 0) {
            throw new IllegalArgumentException("Stock level must be zero or greater");
        }
        if (request.unitPrice() == null || request.unitPrice().signum() < 0) {
            throw new IllegalArgumentException("Unit price must be zero or greater");
        }
        if (request.reorderLevel() < 0) {
            throw new IllegalArgumentException("Reorder level must be zero or greater");
        }
        String batchNumber = request.batchNumber().trim();
        if (inventoryRepository.existsByMedicationIdAndBatchNumberIgnoreCase(
                request.medicationId(), batchNumber)) {
            throw new IllegalStateException("A batch with this batch number already exists for this medication");
        }

        Inventory received = new Inventory.Builder()
                .setInventoryId("INV-" + UUID.randomUUID())
                .setMedicationId(request.medicationId())
                .setStockLevel(request.stockLevel())
                .setExpiryDate(request.expiryDate())
                .setBatchNumber(batchNumber)
                .setSupplier(request.supplier())
                .setUnitPrice(request.unitPrice())
                .setReceivedDate(request.receivedDate() != null ? request.receivedDate() : LocalDate.now())
                .setReorderLevel(request.reorderLevel())
                .build();
        Inventory saved = inventoryRepository.save(received);
        auditLogService.log(actingUsername, "RECEIVE_STOCK", saved.getInventoryId(),
                "before=<none>; after=" + saved + "; reason=RECEIVED");
        return saved;
    }

    @Transactional
    public Inventory adjustStock(String inventoryId, AdjustStockRequest request, String actingUsername) {
        Inventory before = inventoryRepository.findByIdForUpdate(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found: " + inventoryId));
        if (request.delta() == null || request.delta() == 0) {
            throw new IllegalArgumentException("Stock adjustment delta must not be zero");
        }
        LocalDate today = LocalDate.now();
        if (request.reason() == AdjustStockRequest.AdjustmentReason.EXPIRED_WRITE_OFF) {
            if (request.delta() >= 0) {
                throw new IllegalArgumentException("Expired write-off delta must be negative");
            }
            if (!before.getExpiryDate().isBefore(today)) {
                throw new IllegalStateException("Only expired batches can be written off");
            }
            inventoryRepository.writeOffExpiredStock(inventoryId, today);
        } else {
            if (before.getExpiryDate().isBefore(today)) {
                throw new IllegalStateException("Expired stock must be written off, not adjusted");
            }
            int changed = inventoryRepository.adjustStockAtomically(inventoryId, request.delta(), today);
            if (changed == 0) {
                throw new IllegalStateException("Stock adjustment would make the batch stock negative");
            }
        }

        Inventory after = getById(inventoryId);
        int actualDelta = after.getStockLevel() - before.getStockLevel();
        auditLogService.log(actingUsername, "ADJUST_STOCK", inventoryId,
                "before=" + before + "; after=" + after + "; delta=" + actualDelta
                        + "; reason=" + request.reason());
        return after;
    }

    public List<Inventory> getLowStockAlerts() {
        return inventoryRepository.findLowStock(LocalDate.now());
    }

    public List<Inventory> getExpiringSoon(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("Days must be zero or greater");
        }
        LocalDate today = LocalDate.now();
        return inventoryRepository.findExpiringStock(today, today.plusDays(days));
    }

    public List<Inventory> getExpiredStock() {
        return inventoryRepository.findExpiredStock(LocalDate.now());
    }

    public List<InventorySummary> getSummary() {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(30);
        return inventoryRepository.getInventorySummary(today, until).stream().map(row -> {
            long totalStock = ((Number) row[4]).longValue();
            long batchCount = ((Number) row[5]).longValue();
            LocalDate earliestExpiry = (LocalDate) row[6];
            long reorderLevel = ((Number) row[7]).longValue();
            long expiredBatches = ((Number) row[8]).longValue();
            long expiringSoonBatches = ((Number) row[9]).longValue();

            String status;
            if (expiredBatches > 0 && totalStock == 0) {
                status = "EXPIRED";
            } else if (totalStock == 0) {
                status = "OUT";
            } else if (totalStock <= reorderLevel) {
                status = "LOW";
            } else if (expiringSoonBatches > 0) {
                status = "EXPIRING_SOON";
            } else {
                status = "OK";
            }
            return new InventorySummary((String) row[0], (String) row[1], (String) row[2],
                    (String) row[3], totalStock, batchCount, earliestExpiry, status,
                    expiredBatches, expiringSoonBatches);
        }).toList();
    }
}
