package za.ac.cput.medisnyc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import za.ac.cput.medisnyc.domain.Inventory;
import za.ac.cput.medisnyc.domain.Medication;
import za.ac.cput.medisnyc.dto.AdjustStockRequest;
import za.ac.cput.medisnyc.dto.CreateMedicationRequest;
import za.ac.cput.medisnyc.dto.InventorySummary;
import za.ac.cput.medisnyc.dto.ReceiveStockRequest;
import za.ac.cput.medisnyc.dto.UpdateMedicationRequest;
import za.ac.cput.medisnyc.repository.jpa.AuditLogJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.InventoryJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.MedicationJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.OrderItemJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionItemJpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryMedicationServiceTest {

    private final InventoryJpaRepository inventoryRepository = mock(InventoryJpaRepository.class);
    private final MedicationJpaRepository medicationRepository = mock(MedicationJpaRepository.class);
    private final PrescriptionItemJpaRepository prescriptionItems = mock(PrescriptionItemJpaRepository.class);
    private final OrderItemJpaRepository orderItems = mock(OrderItemJpaRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final InventoryService inventoryService =
            new InventoryService(inventoryRepository, medicationRepository, auditLogService);
    private final MedicationService medicationService =
            new MedicationService(medicationRepository, inventoryRepository, prescriptionItems, orderItems, auditLogService);

    @Test
    void rejectsNegativeStock() {
        ReceiveStockRequest request = receiveRequest("MED-1", -1, "B-1");
        when(medicationRepository.existsById("MED-1")).thenReturn(true);

        assertEquals("Stock level must be zero or greater",
                assertThrows(IllegalArgumentException.class,
                        () -> inventoryService.receiveStock(request, "pharmacist")).getMessage());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void rejectsNonexistentMedicationWhenReceivingStock() {
        when(medicationRepository.existsById("MISSING")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.receiveStock(receiveRequest("MISSING", 4, "B-1"), "pharmacist"));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void ignoresClientSuppliedInventoryIdAndAuditsActor() throws Exception {
        ReceiveStockRequest request = new ObjectMapper().registerModule(new JavaTimeModule()).readValue("""
                {
                  "inventoryId":"INV-CLIENT",
                  "medicationId":"MED-1",
                  "stockLevel":5,
                  "expiryDate":"2099-01-01",
                  "batchNumber":"B-1",
                  "unitPrice":3.25,
                  "reorderLevel":2
                }
                """, ReceiveStockRequest.class);
        when(medicationRepository.existsById("MED-1")).thenReturn(true);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inventory saved = inventoryService.receiveStock(request, "pharmacist-user");

        assertTrue(saved.getInventoryId().startsWith("INV-"));
        assertNotEquals("INV-CLIENT", saved.getInventoryId());
        verify(auditLogService).log(eq("pharmacist-user"), eq("RECEIVE_STOCK"), eq(saved.getInventoryId()),
                contains("stockLevel=5"));
    }

    @Test
    void rejectsDuplicateBatchForSameMedication() {
        when(medicationRepository.existsById("MED-1")).thenReturn(true);
        when(inventoryRepository.existsByMedicationIdAndBatchNumberIgnoreCase("MED-1", "B-1")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> inventoryService.receiveStock(receiveRequest("MED-1", 4, "B-1"), "pharmacist"));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void adjustmentBelowZeroReturnsConflictAndUsesAtomicUpdate() {
        Inventory before = batch("INV-1", 3, LocalDate.now().plusDays(2));
        when(inventoryRepository.findByIdForUpdate("INV-1")).thenReturn(Optional.of(before));
        when(inventoryRepository.adjustStockAtomically("INV-1", -4, LocalDate.now())).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> inventoryService.adjustStock("INV-1",
                        new AdjustStockRequest(-4, AdjustStockRequest.AdjustmentReason.DAMAGED), "pharmacist"));

        verify(inventoryRepository).adjustStockAtomically("INV-1", -4, LocalDate.now());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void concurrentAdjustmentPathUsesAtomicUpdateAndAuditsBeforeAndAfter() {
        Inventory before = batch("INV-1", 3, LocalDate.now().plusDays(2));
        Inventory after = batch("INV-1", 5, LocalDate.now().plusDays(2));
        when(inventoryRepository.findByIdForUpdate("INV-1")).thenReturn(Optional.of(before));
        when(inventoryRepository.adjustStockAtomically("INV-1", 2, LocalDate.now())).thenReturn(1);
        when(inventoryRepository.findById("INV-1")).thenReturn(Optional.of(after));

        Inventory result = inventoryService.adjustStock("INV-1",
                new AdjustStockRequest(2, AdjustStockRequest.AdjustmentReason.STOCK_TAKE), "pharmacist-user");

        assertEquals(5, result.getStockLevel());
        verify(inventoryRepository, times(1)).adjustStockAtomically("INV-1", 2, LocalDate.now());
        verify(inventoryRepository, never()).save(any());
        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq("pharmacist-user"), eq("ADJUST_STOCK"), eq("INV-1"), details.capture());
        assertTrue(details.getValue().contains("stockLevel=3"));
        assertTrue(details.getValue().contains("stockLevel=5"));
        assertTrue(details.getValue().contains("reason=STOCK_TAKE"));
    }

    @Test
    void expiredWriteOffZerosBatchAndAuditsTheActualChange() {
        Inventory before = batch("INV-EXPIRED", 3, LocalDate.now().minusDays(1));
        Inventory after = batch("INV-EXPIRED", 0, LocalDate.now().minusDays(1));
        when(inventoryRepository.findByIdForUpdate("INV-EXPIRED")).thenReturn(Optional.of(before));
        when(inventoryRepository.writeOffExpiredStock("INV-EXPIRED", LocalDate.now())).thenReturn(1);
        when(inventoryRepository.findById("INV-EXPIRED")).thenReturn(Optional.of(after));

        Inventory result = inventoryService.adjustStock("INV-EXPIRED",
                new AdjustStockRequest(-3, AdjustStockRequest.AdjustmentReason.EXPIRED_WRITE_OFF), "pharmacist-user");

        assertEquals(0, result.getStockLevel());
        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq("pharmacist-user"), eq("ADJUST_STOCK"), eq("INV-EXPIRED"), details.capture());
        assertTrue(details.getValue().contains("delta=-3"));
        assertTrue(details.getValue().contains("reason=EXPIRED_WRITE_OFF"));
    }

    @Test
    void blocksMedicationDeletionWhenReferencedByPrescription() {
        Medication medication = medication("MED-1");
        when(medicationRepository.findById("MED-1")).thenReturn(Optional.of(medication));
        when(inventoryRepository.existsByMedicationIdAndStockLevelGreaterThan("MED-1", 0)).thenReturn(false);
        when(prescriptionItems.existsByMedicationId("MED-1")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> medicationService.deleteMedicine("MED-1", "admin"));

        verify(medicationRepository, never()).deleteById(anyString());
        verify(inventoryRepository, never()).deleteByMedicationIdAndStockLevelLessThanEqual(anyString(), anyInt());
    }

    @Test
    void excludesExpiredQuantityFromSummary() {
        Object[] expiredOnly = {
                "MED-1", "Medicine", "Tablet", "10 mg", 0L, 1L, null, 5, 1L, 0L
        };
        when(inventoryRepository.getInventorySummary(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.<Object[]>of(expiredOnly));

        InventorySummary summary = inventoryService.getSummary().get(0);

        assertEquals(0, summary.totalStock());
        assertEquals("EXPIRED", summary.status());
        assertEquals(1, summary.expiredBatchCount());
        verify(inventoryRepository).getInventorySummary(LocalDate.now(), LocalDate.now().plusDays(30));
    }

    @Test
    void medicationUpdateUsesPathIdInsteadOfRequestBodyId() {
        Medication before = medication("MED-PATH");
        when(medicationRepository.findById("MED-PATH")).thenReturn(Optional.of(before));
        when(medicationRepository.existsDuplicate(anyString(), anyString(), anyString(), eq("MED-PATH"))).thenReturn(false);
        when(medicationRepository.save(any(Medication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Medication result = medicationService.updateMedicine("MED-PATH",
                new UpdateMedicationRequest("New name", "10 mg", "Tablet", "Analgesic", null, null), "admin");

        assertEquals("MED-PATH", result.getMedicationId());
    }

    @Test
    void rejectsDuplicateMedicationNameStrengthAndForm() {
        when(medicationRepository.existsDuplicate("Medicine", "10 mg", "Tablet", null)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> medicationService.addMedicine(
                new CreateMedicationRequest("Medicine", "10 mg", "Tablet", "Analgesic", null, null), "admin"));
        verify(medicationRepository, never()).save(any());
    }

    private ReceiveStockRequest receiveRequest(String medicationId, int stock, String batchNumber) {
        return new ReceiveStockRequest(medicationId, stock, LocalDate.now().plusDays(2), batchNumber,
                "Supplier", new BigDecimal("2.00"), LocalDate.now(), 5);
    }

    private Inventory batch(String inventoryId, int stock, LocalDate expiryDate) {
        return new Inventory.Builder()
                .setInventoryId(inventoryId)
                .setMedicationId("MED-1")
                .setStockLevel(stock)
                .setExpiryDate(expiryDate)
                .setBatchNumber("B-1")
                .setUnitPrice(new BigDecimal("2.00"))
                .setReorderLevel(5)
                .build();
    }

    private Medication medication(String medicationId) {
        return new Medication.Builder()
                .setMedicationId(medicationId)
                .setMedicationName("Medicine")
                .setStrength("10 mg")
                .setDosageForm("Tablet")
                .setCategory("Analgesic")
                .build();
    }
}
