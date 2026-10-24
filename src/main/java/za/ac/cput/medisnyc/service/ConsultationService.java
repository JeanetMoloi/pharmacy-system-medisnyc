package za.ac.cput.medisnyc.service;

/* ConsultationService.java
   Module 3: Doctor Consultation & Prescription Module.
*/

import za.ac.cput.medisnyc.domain.MedicalRecord;
import za.ac.cput.medisnyc.domain.Prescription;
import za.ac.cput.medisnyc.domain.PrescriptionProcessingStatus;
import za.ac.cput.medisnyc.domain.PrescriptionItem;
import za.ac.cput.medisnyc.domain.PrescriptionStatus;
import za.ac.cput.medisnyc.repository.jpa.MedicalRecordJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PatientJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionItemJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConsultationService {

    private final MedicalRecordJpaRepository medicalRecordRepository;
    private final PrescriptionJpaRepository prescriptionRepository;
    private final PrescriptionItemJpaRepository prescriptionItemRepository;
    private final PatientJpaRepository patientRepository;

    @Autowired
    public ConsultationService(MedicalRecordJpaRepository medicalRecordRepository,
                               PrescriptionJpaRepository prescriptionRepository,
                               PrescriptionItemJpaRepository prescriptionItemRepository,
                               PatientJpaRepository patientRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.patientRepository = patientRepository;
    }

    // Today's appointments handled by AppointmentService.getByDoctor + date filter in controller.

    @Transactional
    public MedicalRecord createMedicalRecord(MedicalRecord record) {
        validatePatient(record.getPatientId());
        return medicalRecordRepository.save(record);
    }

    public List<MedicalRecord> getRecordsByPatient(String patientId) {
        return medicalRecordRepository.findByPatientId(patientId);
    }

    public List<MedicalRecord> getRecordsByDoctor(String doctorId) {
        return medicalRecordRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public Prescription createPrescription(Prescription prescription, List<PrescriptionItem> items) {
        validatePatient(prescription.getPatientId());
        Prescription toSave = new Prescription.Builder()
                .setPrescriptionId("RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .setPatientId(prescription.getPatientId())
                .setDoctorId(prescription.getDoctorId())
                .setDateIssued(prescription.getDateIssued())
                .setExpiryDate(prescription.getExpiryDate())
                .setInstructions(prescription.getInstructions())
                .setRefillsAllowed(prescription.getRefillsAllowed())
                .setRefillsUsed(0)
                .setStatus(PrescriptionStatus.ACTIVE)
                .setProcessingStatus(PrescriptionProcessingStatus.PENDING)
                .build();

        Prescription saved = prescriptionRepository.save(toSave);

        if (items != null) {
            for (PrescriptionItem item : items) {
                PrescriptionItem itemToSave = new PrescriptionItem.Builder()
                        .setPrescriptionItemId("RXI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .setPrescriptionId(saved.getPrescriptionId())
                        .setMedicationId(item.getMedicationId())
                        .setQuantity(item.getQuantity())
                        .setDosage(item.getDosage())
                        .setFrequency(item.getFrequency())
                        .setDurationDays(item.getDurationDays())
                        .build();
                prescriptionItemRepository.save(itemToSave);
            }
        }

        return saved;
    }

    public List<PrescriptionItem> getItemsForPrescription(String prescriptionId) {
        return prescriptionItemRepository.findByPrescriptionId(prescriptionId);
    }

    private void validatePatient(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException("Patient not found: " + patientId);
        }
    }
}
