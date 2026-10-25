package za.ac.cput.medisnyc.service;

import za.ac.cput.medisnyc.domain.Medication;
import za.ac.cput.medisnyc.dto.CreateMedicationRequest;
import za.ac.cput.medisnyc.dto.UpdateMedicationRequest;
import za.ac.cput.medisnyc.exception.ResourceNotFoundException;
import za.ac.cput.medisnyc.repository.jpa.InventoryJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.MedicationJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.OrderItemJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionItemJpaRepository;
import za.ac.cput.medisnyc.util.Helper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .setMedicationId(Helper.generateId("MED"))
                .setMedicationName(request.medicationName().trim())
                .setDosageForm(request.dosageForm().trim())
                .setManufacturer(request.manufacturer())
                .setStrength(request.strength().trim())
                .setDescription(request.description())
                .setCategory(request.category().trim())
                .build();
        Medication saved = medicationRepository.save(medication);
        auditLogService.log(actingUsername, "CREATE_MEDICATION", saved.getMedicationId(),
                "before=<none>; after=" + saved + "; reason=MEDICATION_CREATED");
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
                "before=" + before + "; after=" + saved + "; reason=MEDICATION_UPDATED");
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
                "before=" + medication + "; after=<deleted>; reason=MEDICATION_DELETED");
    }

    public List<Medication> search(String name) {
        return medicationRepository.findByMedicationNameContainingIgnoreCase(name);
    }

    private void rejectDuplicate(String name, String strength, String dosageForm, String excludedId) {
        if (medicationRepository.existsDuplicate(name.trim(), strength.trim(), dosageForm.trim(), excludedId)) {
            throw new IllegalStateException("A medication with the same name, strength, and dosage form already exists");
        }
    }
}
