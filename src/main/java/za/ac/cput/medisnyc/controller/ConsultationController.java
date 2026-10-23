package za.ac.cput.medisnyc.controller;

/* ConsultationController.java
   Module 3: Doctor Consultation & Prescription Module.
   Author: Lisakhanya Mpahla
*/

import za.ac.cput.medisnyc.domain.MedicalRecord;
import za.ac.cput.medisnyc.domain.Appointment;
import za.ac.cput.medisnyc.domain.Prescription;
import za.ac.cput.medisnyc.domain.PrescriptionItem;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.service.AppointmentService;
import za.ac.cput.medisnyc.service.ConsultationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ConsultationController {

    private final ConsultationService consultationService;
    private final AppointmentService appointmentService;

    @Autowired
    public ConsultationController(ConsultationService consultationService, AppointmentService appointmentService) {
        this.consultationService = consultationService;
        this.appointmentService = appointmentService;
    }

    @PostMapping("/api/medical-records")
    public ResponseEntity<MedicalRecord> createMedicalRecord(@RequestBody MedicalRecord record,
                                                              @AuthenticationPrincipal User user) {
        String doctorId = getAuthenticatedDoctorId(user, record.getDoctorId());
        if (record.getAppointmentId() != null && !record.getAppointmentId().isBlank()) {
            Appointment appointment = appointmentService.getById(record.getAppointmentId());
            if (!appointment.getDoctorId().equals(doctorId)
                    || !appointment.getPatientId().equals(record.getPatientId())) {
                throw new AccessDeniedException("Appointment does not belong to this doctor and patient");
            }
        }
        MedicalRecord authenticatedRecord = new MedicalRecord.Builder()
                .copy(record)
                .setDoctorId(doctorId)
                .build();
        return ResponseEntity.ok(consultationService.createMedicalRecord(authenticatedRecord));
    }

    @GetMapping("/api/medical-records/patient/{patientId}")
    public ResponseEntity<List<MedicalRecord>> getByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(consultationService.getRecordsByPatient(patientId));
    }

    @GetMapping("/api/medical-records/doctor/{doctorId}")
    public ResponseEntity<List<MedicalRecord>> getByDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(consultationService.getRecordsByDoctor(doctorId));
    }

    public static class PrescriptionRequest {
        public Prescription prescription;
        public List<PrescriptionItem> items;
    }

    @PostMapping("/api/consultations/prescriptions")
    public ResponseEntity<Prescription> createPrescription(@RequestBody PrescriptionRequest request,
                                                            @AuthenticationPrincipal User user) {
        String doctorId = getAuthenticatedDoctorId(user, request.prescription.getDoctorId());
        Prescription prescription = request.prescription;
        Prescription authenticatedPrescription = new Prescription.Builder()
                .setPrescriptionId(prescription.getPrescriptionId())
                .setPatientId(prescription.getPatientId())
                .setDoctorId(doctorId)
                .setDateIssued(prescription.getDateIssued())
                .setExpiryDate(prescription.getExpiryDate())
                .setInstructions(prescription.getInstructions())
                .setRefillsAllowed(prescription.getRefillsAllowed())
                .setRefillsUsed(prescription.getRefillsUsed())
                .setStatus(prescription.getStatus())
                .setProcessingStatus(prescription.getProcessingStatus())
                .build();
        return ResponseEntity.ok(consultationService.createPrescription(authenticatedPrescription, request.items));
    }

    @GetMapping("/api/consultations/prescriptions/{prescriptionId}/items")
    public ResponseEntity<List<PrescriptionItem>> getItems(@PathVariable String prescriptionId) {
        return ResponseEntity.ok(consultationService.getItemsForPrescription(prescriptionId));
    }

    private String getAuthenticatedDoctorId(User user, String requestedDoctorId) {
        boolean doctor = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DOCTOR"::equals);
        if (!doctor) {
            return requestedDoctorId;
        }
        if (user.getLinkedProfileId() == null || user.getLinkedProfileId().isBlank()) {
            throw new AccessDeniedException("Doctor account has no linked doctor profile");
        }
        return user.getLinkedProfileId();
    }
}
