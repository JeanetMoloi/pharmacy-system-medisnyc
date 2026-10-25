package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import za.ac.cput.medisnyc.domain.Medication;
import za.ac.cput.medisnyc.dto.CreateMedicationRequest;
import za.ac.cput.medisnyc.dto.InventorySummary;
import za.ac.cput.medisnyc.dto.ReceiveStockRequest;
import za.ac.cput.medisnyc.repository.jpa.AuditLogJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.InventoryJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.MedicationJpaRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MedicationDataSeederTest {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "Analgesic", "Antibiotic", "Antihistamine", "Antacid", "Antidiabetic",
            "Antihypertensive", "Antiviral", "Vitamin", "Vaccine");
    private static final Set<String> ALLOWED_UNITS = Set.of("mg", "g", "ml", "mcg", "IU", "OTHER");
    private static final Set<String> ALLOWED_FORMS = Set.of(
            "Tablet", "Capsule", "Syrup", "Injection", "Cream", "Drops", "Inhaler",
            "Ointment", "Solution", "Suspension", "Powder", "Patch");

    @Test
    void providesAtLeastFiveValidMedicinesForEveryFormCategory() {
        List<MedicationDataSeeder.MedicationSeed> samples = MedicationDataSeeder.sampleMedications();
        assertEquals(ALLOWED_CATEGORIES, samples.stream().map(MedicationDataSeeder.MedicationSeed::category)
                .collect(Collectors.toSet()));
        for (String category : ALLOWED_CATEGORIES) {
            assertTrue(samples.stream().filter(sample -> sample.category().equals(category)).count() >= 5,
                    "Expected at least five medicines in " + category);
        }
        assertTrue(samples.stream().allMatch(sample -> ALLOWED_UNITS.contains(sample.strengthUnit())));
        assertTrue(samples.stream().allMatch(sample -> ALLOWED_FORMS.contains(sample.dosageForm())));
        assertTrue(samples.stream().allMatch(sample -> sample.strength().matches("\\d+(\\.\\d+)? (mg|g|ml|mcg|IU)")
                || sample.strength().equals("5%")));
    }

    @Test
    void everyMedicineHasAtLeastFiveUnitsAndExactlyTwoAreLowWithOneExpiringBatch() {
        List<MedicationDataSeeder.MedicationSeed> samples = MedicationDataSeeder.sampleMedications();
        long lowMedicines = samples.stream()
                .filter(sample -> sample.firstBatchStock() + sample.secondBatchStock() <= 10)
                .count();
        long expiringBatches = samples.stream()
                .filter(sample -> sample.firstBatchStock() > 0 && sample.expiryInDays() <= 30)
                .count();

        assertTrue(samples.stream().allMatch(sample ->
                sample.firstBatchStock() + sample.secondBatchStock() >= 5));
        assertEquals(2, lowMedicines);
        assertEquals(1, expiringBatches);
        assertTrue(samples.stream().filter(sample -> sample.firstBatchStock() + sample.secondBatchStock() > 10)
                .allMatch(sample -> sample.firstBatchStock() + sample.secondBatchStock() <= 300));
        assertTrue(samples.stream().allMatch(sample -> new java.math.BigDecimal(sample.unitPrice()).signum() > 0));

        LocalDate today = LocalDate.now();
        List<Object[]> summaryRows = samples.stream().map(sample -> {
            long stock = sample.firstBatchStock() + sample.secondBatchStock();
            long batches = sample.secondBatchStock() > 0 ? 2 : 1;
            long expiring = sample.expiryInDays() <= 30 ? 1 : 0;
            return new Object[]{
                    sample.name(), sample.name(), sample.dosageForm(), sample.strength(),
                    stock, batches, today.plusDays(sample.expiryInDays()), 10L, 0L, expiring
            };
        }).toList();
        InventoryJpaRepository inventoryRepository = mock(InventoryJpaRepository.class);
        when(inventoryRepository.getInventorySummary(today, today.plusDays(30))).thenReturn(summaryRows);
        InventoryService inventoryService = new InventoryService(
                inventoryRepository, mock(MedicationJpaRepository.class),
                new AuditLogService(mock(AuditLogJpaRepository.class)));
        List<InventorySummary> inventorySummary = inventoryService.getSummary();
        assertEquals(2, inventorySummary.stream().filter(summary -> summary.status().equals("LOW")).count());
        assertEquals(1, inventorySummary.stream().filter(summary -> summary.status().equals("EXPIRING_SOON")).count());
    }

    @Test
    void runningSeederTwiceAddsNoDuplicatesAndUsesInventoryReceivePath() {
        MedicationJpaRepository medications = mock(MedicationJpaRepository.class);
        InventoryJpaRepository inventory = mock(InventoryJpaRepository.class);
        MedicationService medicationService = mock(MedicationService.class);
        InventoryService inventoryService = mock(InventoryService.class);
        Set<String> storedMedicines = new HashSet<>();
        Set<String> storedBatches = new HashSet<>();
        List<ReceiveStockRequest> receivedBatches = new ArrayList<>();

        when(medications.existsDuplicate(anyString(), anyString(), anyString(), isNull()))
                .thenAnswer(invocation -> storedMedicines.contains(key(
                        invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))));
        when(inventory.existsByBatchNumberIgnoreCase(anyString()))
                .thenAnswer(invocation -> storedBatches.contains(((String) invocation.getArgument(0)).toLowerCase(Locale.ROOT)));
        when(medicationService.addMedicine(any(CreateMedicationRequest.class), eq("system")))
                .thenAnswer(invocation -> {
                    CreateMedicationRequest request = invocation.getArgument(0);
                    storedMedicines.add(key(request.medicationName(), request.strength(), request.dosageForm()));
                    return new Medication.Builder()
                            .setMedicationId("MED-" + storedMedicines.size())
                            .setMedicationName(request.medicationName())
                            .setStrength(request.strength())
                            .setDosageForm(request.dosageForm())
                            .setCategory(request.category())
                            .setManufacturer(request.manufacturer())
                            .setDescription(request.description())
                            .build();
                });
        when(inventoryService.receiveStock(any(ReceiveStockRequest.class), eq("system")))
                .thenAnswer(invocation -> {
                    ReceiveStockRequest request = invocation.getArgument(0);
                    storedBatches.add(request.batchNumber().toLowerCase(Locale.ROOT));
                    receivedBatches.add(request);
                    return null;
                });

        MedicationDataSeeder seeder = new MedicationDataSeeder(
                medications, inventory, medicationService, inventoryService, true);
        seeder.run();
        int firstRunMedicines = storedMedicines.size();
        int firstRunBatches = receivedBatches.size();
        seeder.run();

        assertEquals(MedicationDataSeeder.sampleMedications().size(), firstRunMedicines);
        assertEquals(firstRunMedicines, storedMedicines.size());
        assertEquals(firstRunBatches, receivedBatches.size());
        assertEquals(45, firstRunMedicines);
        assertTrue(firstRunBatches >= firstRunMedicines);
        assertTrue(firstRunBatches <= firstRunMedicines * 2);
        assertTrue(receivedBatches.stream().allMatch(batch ->
                batch.receivedDate().equals(LocalDate.now())
                        && (batch.expiryDate().isAfter(LocalDate.now().plusMonths(6))
                            || batch.expiryDate().equals(LocalDate.now().plusDays(20)))
                        && !batch.batchNumber().isBlank()
                        && !batch.supplier().isBlank()));
        verify(medicationService, times(firstRunMedicines)).addMedicine(any(CreateMedicationRequest.class), eq("system"));
        verify(inventoryService, times(firstRunBatches)).receiveStock(any(ReceiveStockRequest.class), eq("system"));
    }

    @Test
    void continuesSeedingAfterOneMedicineFails() {
        MedicationJpaRepository medications = mock(MedicationJpaRepository.class);
        InventoryJpaRepository inventory = mock(InventoryJpaRepository.class);
        MedicationService medicationService = mock(MedicationService.class);
        InventoryService inventoryService = mock(InventoryService.class);
        AtomicInteger medicineCalls = new AtomicInteger();

        when(medications.existsDuplicate(anyString(), anyString(), anyString(), isNull())).thenReturn(false);
        when(inventory.existsByBatchNumberIgnoreCase(anyString())).thenReturn(false);
        when(medicationService.addMedicine(any(CreateMedicationRequest.class), eq("system")))
                .thenAnswer(invocation -> {
                    if (medicineCalls.getAndIncrement() == 0) {
                        throw new IllegalStateException("Invalid sample");
                    }
                    CreateMedicationRequest request = invocation.getArgument(0);
                    return new Medication.Builder()
                            .setMedicationId("MED-" + medicineCalls.get())
                            .setMedicationName(request.medicationName())
                            .setStrength(request.strength())
                            .setDosageForm(request.dosageForm())
                            .setCategory(request.category())
                            .build();
                });

        new MedicationDataSeeder(medications, inventory, medicationService, inventoryService, true).run();

        assertEquals(MedicationDataSeeder.sampleMedications().size(),
                medicineCalls.get());
        verify(inventoryService, times(MedicationDataSeeder.sampleMedications().subList(1,
                        MedicationDataSeeder.sampleMedications().size()).stream()
                .mapToInt(sample -> sample.secondBatchStock() > 0 ? 2 : 1).sum()))
                .receiveStock(any(ReceiveStockRequest.class), eq("system"));
    }

    @Test
    void disabledFlagDoesNotAddMedicinesOrBatches() {
        MedicationJpaRepository medications = mock(MedicationJpaRepository.class);
        InventoryJpaRepository inventory = mock(InventoryJpaRepository.class);
        MedicationService medicationService = mock(MedicationService.class);
        InventoryService inventoryService = mock(InventoryService.class);

        new MedicationDataSeeder(medications, inventory, medicationService, inventoryService, false).run();

        verifyNoInteractions(medications, inventory, medicationService, inventoryService);
    }

    private String key(String name, String strength, String form) {
        return (name + "|" + strength + "|" + form).toLowerCase(Locale.ROOT);
    }
}
