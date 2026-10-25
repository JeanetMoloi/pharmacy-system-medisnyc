package za.ac.cput.medisnyc.service;

import za.ac.cput.medisnyc.domain.Medication;
import za.ac.cput.medisnyc.dto.CreateMedicationRequest;
import za.ac.cput.medisnyc.dto.UpdateMedicationRequest;
import za.ac.cput.medisnyc.exception.ResourceNotFoundException;
import za.ac.cput.medisnyc.repository.jpa.InventoryJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.MedicationJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.OrderItemJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionItemJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MedicationService {

    private final MedicationJpaRepository medicationRepository;
    private final InventoryJpaRepository inventoryRepository;
    private final PrescriptionItemJpaRepository prescriptionItemRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final AuditLogService auditLogService;

    public MedicationService(MedicationJpaRepository medicationRepository,
                             InventoryJpaRepository inventoryRepository,
                             PrescriptionItemJpaRepository prescriptionItemRepository,
                             OrderItemJpaRepository orderItemRepository,
                             AuditLogService auditLogService) {
        this.medicationRepository = medicationRepository;
        this.inventoryRepository = inventoryRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.auditLogService = auditLogService;
    }

    public List<Medication> getAll() {
        return medicationRepository.findAll();
    }

    public Medication getById(String medicationId) {
        return medicationRepository.findById(medicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication not found: " + medicationId));
    }

    @Transactional
    public Medication addMedicine(CreateMedicationRequest request, String actingUsername) {
        rejectDuplicate(request.medicationName(), request.strength(), request.dosageForm(), null);
        Medication medication = new Medication.Builder()
                .setMedicationId("MED-" + UUID.randomUUID())
                .setMedicationName(request.medicationName().trim())
                .setDosageForm(request.dosageForm().trim())
                .setManufacturer(request.manufacturer())
                .setStrength(request.strength().trim())
                .setDescription(request.description())
                .setCategory(request.category().trim())
                .build();
        Medication saved = medicationRepository.save(medication);
        auditLogService.log(actingUsername, "CREATE_MEDICATION", saved.getMedicationId(),
                medicationAuditSummary(saved) + "; reason=MEDICATION_CREATED");
        return saved;
    }

    @Transactional
    public Medication updateMedicine(String medicationId, UpdateMedicationRequest request, String actingUsername) {
        Medication before = getById(medicationId);
        rejectDuplicate(request.medicationName(), request.strength(), request.dosageForm(), medicationId);
        Medication updated = new Medication.Builder()
                .setMedicationId(medicationId)
                .setMedicationName(request.medicationName().trim())
                .setDosageForm(request.dosageForm().trim())
                .setManufacturer(request.manufacturer())
                .setStrength(request.strength().trim())
                .setDescription(request.description())
                .setCategory(request.category().trim())
                .build();
        Medication saved = medicationRepository.save(updated);
        auditLogService.log(actingUsername, "UPDATE_MEDICATION", medicationId,
                medicationAuditSummary(saved) + "; changed=" + changedMedicationFields(before, saved)
                        + "; reason=MEDICATION_UPDATED");
        return saved;
    }

    @Transactional
    public void deleteMedicine(String medicationId, String actingUsername) {
        Medication medication = getById(medicationId);
        if (inventoryRepository.existsByMedicationIdAndStockLevelGreaterThan(medicationId, 0)
                || prescriptionItemRepository.existsByMedicationId(medicationId)
                || orderItemRepository.existsByMedicationId(medicationId)) {
            throw new IllegalStateException(
                    "Medication cannot be deleted because stock, a prescription, or an order references it");
        }
        inventoryRepository.deleteByMedicationIdAndStockLevelLessThanEqual(medicationId, 0);
        medicationRepository.deleteById(medicationId);
        auditLogService.log(actingUsername, "DELETE_MEDICATION", medicationId,
                medicationAuditSummary(medication) + "; reason=MEDICATION_DELETED");
    }

    public List<Medication> search(String name) {
        return medicationRepository.findByMedicationNameContainingIgnoreCase(name);
    }

    private void rejectDuplicate(String name, String strength, String dosageForm, String excludedId) {
        if (medicationRepository.existsDuplicate(name.trim(), strength.trim(), dosageForm.trim(), excludedId)) {
            throw new IllegalStateException("A medication with the same name, strength, and dosage form already exists");
        }
    }

    private String medicationAuditSummary(Medication medication) {
        return "medicationId=" + medication.getMedicationId()
                + "; name=" + medication.getMedicationName()
                + "; strength=" + medication.getStrength()
                + "; form=" + medication.getDosageForm();
    }

    private List<String> changedMedicationFields(Medication before, Medication after) {
        List<String> changes = new ArrayList<>();
        addChange(changes, "name", before.getMedicationName(), after.getMedicationName());
        addChange(changes, "strength", before.getStrength(), after.getStrength());
        addChange(changes, "form", before.getDosageForm(), after.getDosageForm());
        addChange(changes, "category", before.getCategory(), after.getCategory());
        addChange(changes, "manufacturer", before.getManufacturer(), after.getManufacturer());
        addChange(changes, "description", before.getDescription(), after.getDescription());
        return changes;
    }

    private void addChange(List<String> changes, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(field + ":" + before + "->" + after);
        }
    }
}
