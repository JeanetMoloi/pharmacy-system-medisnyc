package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import za.ac.cput.medisnyc.domain.MedicalRecord;
import za.ac.cput.medisnyc.domain.Prescription;
import za.ac.cput.medisnyc.domain.PrescriptionProcessingStatus;
import za.ac.cput.medisnyc.domain.PrescriptionStatus;
import za.ac.cput.medisnyc.repository.jpa.MedicalRecordJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PatientJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionItemJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionJpaRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultationServiceTest {

    @Test
    void savesMedicalRecordForExistingPatient() {
        MedicalRecordJpaRepository records = mock(MedicalRecordJpaRepository.class);
        PatientJpaRepository patients = mock(PatientJpaRepository.class);
        when(patients.existsById("PAT-1")).thenReturn(true);
        MedicalRecord record = new MedicalRecord.Builder()
                .setRecordId("MR-1")
                .setPatientId("PAT-1")
                .setDoctorId("DOC-1")
                .build();
        when(records.save(record)).thenReturn(record);
        ConsultationService service = service(records, mock(PrescriptionJpaRepository.class), patients);

        assertEquals(record, service.createMedicalRecord(record));
        verify(records).save(record);
    }

    @Test
    void createsPrescriptionWithServerIdAndDefaultStatuses() {
        PrescriptionJpaRepository prescriptions = mock(PrescriptionJpaRepository.class);
        PatientJpaRepository patients = mock(PatientJpaRepository.class);
        when(patients.existsById("PAT-1")).thenReturn(true);
        when(prescriptions.save(any(Prescription.class))).thenAnswer(call -> call.getArgument(0));
        Prescription submitted = new Prescription.Builder()
                .setPrescriptionId("RX-CLIENT")
                .setPatientId("PAT-1")
                .setDoctorId("DOC-1")
                .setDateIssued(LocalDate.now())
                .setExpiryDate(LocalDate.now().plusDays(30))
                .setRefillsUsed(5)
                .setStatus(PrescriptionStatus.CANCELLED)
                .setProcessingStatus(PrescriptionProcessingStatus.COLLECTED)
                .build();
        ConsultationService service = service(mock(MedicalRecordJpaRepository.class), prescriptions, patients);

        Prescription saved = service.createPrescription(submitted, List.of());

        org.junit.jupiter.api.Assertions.assertTrue(saved.getPrescriptionId().startsWith("RX-"));
        org.junit.jupiter.api.Assertions.assertNotEquals("RX-CLIENT", saved.getPrescriptionId());
        assertEquals(PrescriptionStatus.ACTIVE, saved.getStatus());
        assertEquals(PrescriptionProcessingStatus.PENDING, saved.getProcessingStatus());
        assertEquals(0, saved.getRefillsUsed());
    }

    @Test
    void rejectsBlankOrUnknownPatientBeforeSaving() throws ReflectiveOperationException {
        MedicalRecordJpaRepository records = mock(MedicalRecordJpaRepository.class);
        PatientJpaRepository patients = mock(PatientJpaRepository.class);
        ConsultationService service = service(records, mock(PrescriptionJpaRepository.class), patients);
        MedicalRecord blankPatient = new MedicalRecord.Builder()
                .setRecordId("MR-1")
                .setPatientId("PAT-1")
                .setDoctorId("DOC-1")
                .build();
        java.lang.reflect.Field patientId = MedicalRecord.class.getDeclaredField("patientId");
        patientId.setAccessible(true);
        patientId.set(blankPatient, " ");
        MedicalRecord unknownPatient = new MedicalRecord.Builder()
                .setRecordId("MR-2")
                .setPatientId("MISSING")
                .setDoctorId("DOC-1")
                .build();

        assertEquals("Patient ID is required",
                assertThrows(IllegalArgumentException.class, () -> service.createMedicalRecord(blankPatient)).getMessage());
        assertEquals("Patient not found: MISSING",
                assertThrows(IllegalArgumentException.class, () -> service.createMedicalRecord(unknownPatient)).getMessage());
        verify(records, never()).save(any(MedicalRecord.class));
    }

    @Test
    void rejectsPrescriptionForUnknownPatient() {
        PrescriptionJpaRepository prescriptions = mock(PrescriptionJpaRepository.class);
        PatientJpaRepository patients = mock(PatientJpaRepository.class);
        Prescription submitted = new Prescription.Builder()
                .setPatientId("MISSING")
                .setDoctorId("DOC-1")
                .build();
        ConsultationService service = service(mock(MedicalRecordJpaRepository.class), prescriptions, patients);

        assertEquals("Patient not found: MISSING", assertThrows(IllegalArgumentException.class,
                () -> service.createPrescription(submitted, List.of())).getMessage());
        verify(prescriptions, never()).save(any(Prescription.class));
    }

    private ConsultationService service(MedicalRecordJpaRepository records,
                                        PrescriptionJpaRepository prescriptions,
                                        PatientJpaRepository patients) {
        return new ConsultationService(records, prescriptions, mock(PrescriptionItemJpaRepository.class), patients);
    }
}
