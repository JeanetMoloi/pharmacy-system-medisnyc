package za.ac.cput.medisnyc.controller;

import org.junit.jupiter.api.Test;
import za.ac.cput.medisnyc.domain.Appointment;
import za.ac.cput.medisnyc.domain.AppointmentStatus;
import za.ac.cput.medisnyc.domain.MedicalRecord;
import za.ac.cput.medisnyc.domain.Prescription;
import za.ac.cput.medisnyc.domain.PrescriptionProcessingStatus;
import za.ac.cput.medisnyc.domain.PrescriptionStatus;
import za.ac.cput.medisnyc.domain.Role;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.repository.jpa.AppointmentJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.MedicalRecordJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PatientJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PatientJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionItemJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionJpaRepository;
import za.ac.cput.medisnyc.service.AppointmentService;
import za.ac.cput.medisnyc.service.ConsultationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultationControllerTest {

    @Test
    void createMedicalRecordUsesAuthenticatedDoctorsId() {
        MedicalRecordJpaRepository records = mock(MedicalRecordJpaRepository.class);
        ConsultationController controller = controller(records, mock(PrescriptionJpaRepository.class),
                mock(AppointmentJpaRepository.class));
        AtomicReference<MedicalRecord> savedRecord = new AtomicReference<>();
        when(records.save(any(MedicalRecord.class))).thenAnswer(call -> {
            MedicalRecord saved = call.getArgument(0);
            savedRecord.set(saved);
            return saved;
        });
        MedicalRecord request = new MedicalRecord.Builder()
                .setRecordId("MR-CLIENT")
                .setPatientId("PAT-1")
                .setDoctorId("DOC-SPOOFED")
                .setDiagnosis("Example")
                .build();

        controller.createMedicalRecord(request, doctorUser("DOC-AUTHENTICATED"));

        assertEquals("DOC-AUTHENTICATED", savedRecord.get().getDoctorId());
        assertTrue(savedRecord.get().getRecordId().startsWith("MR-"));
        assertNotEquals("MR-CLIENT", savedRecord.get().getRecordId());
    }

    @Test
    void createMedicalRecordGeneratesIdWhenClientOmitsIt() throws ReflectiveOperationException {
        MedicalRecordJpaRepository records = mock(MedicalRecordJpaRepository.class);
        ConsultationController controller = controller(records, mock(PrescriptionJpaRepository.class),
                mock(AppointmentJpaRepository.class));
        when(records.save(any(MedicalRecord.class))).thenAnswer(call -> call.getArgument(0));
        MedicalRecord request = new MedicalRecord.Builder()
                .setRecordId("MR-TEMP")
                .setPatientId("PAT-1")
                .setDoctorId("DOC-SPOOFED")
                .build();
        setRecordField(request, "recordId", null);

        MedicalRecord saved = controller.createMedicalRecord(request, doctorUser("DOC-AUTHENTICATED")).getBody();

        assertTrue(saved.getRecordId().startsWith("MR-"));
        assertEquals("DOC-AUTHENTICATED", saved.getDoctorId());
    }

    @Test
    void createPrescriptionUsesAuthenticatedDoctorsId() {
        PrescriptionJpaRepository prescriptions = mock(PrescriptionJpaRepository.class);
        ConsultationController controller = controller(mock(MedicalRecordJpaRepository.class), prescriptions,
                mock(AppointmentJpaRepository.class));
        AtomicReference<Prescription> savedPrescription = new AtomicReference<>();
        when(prescriptions.save(any(Prescription.class))).thenAnswer(call -> {
            Prescription saved = call.getArgument(0);
            savedPrescription.set(saved);
            return saved;
        });
        ConsultationController.PrescriptionRequest request = new ConsultationController.PrescriptionRequest();
        request.prescription = new Prescription.Builder()
                .setPrescriptionId("RX-CLIENT")
                .setPatientId("PAT-1")
                .setDoctorId("DOC-SPOOFED")
                .setDateIssued(LocalDate.now())
                .setExpiryDate(LocalDate.now().plusDays(30))
                .setRefillsUsed(2)
                .setStatus(PrescriptionStatus.CANCELLED)
                .setProcessingStatus(PrescriptionProcessingStatus.COLLECTED)
                .build();
        request.items = List.of();

        controller.createPrescription(request, doctorUser("DOC-AUTHENTICATED"));

        assertEquals("DOC-AUTHENTICATED", savedPrescription.get().getDoctorId());
        assertTrue(savedPrescription.get().getPrescriptionId().startsWith("RX-"));
        assertNotEquals("RX-CLIENT", savedPrescription.get().getPrescriptionId());
        assertEquals(PrescriptionStatus.ACTIVE, savedPrescription.get().getStatus());
        assertEquals(PrescriptionProcessingStatus.PENDING, savedPrescription.get().getProcessingStatus());
        assertEquals(0, savedPrescription.get().getRefillsUsed());
    }

    @Test
    void createPrescriptionGeneratesIdWhenClientOmitsIt() {
        PrescriptionJpaRepository prescriptions = mock(PrescriptionJpaRepository.class);
        ConsultationController controller = controller(mock(MedicalRecordJpaRepository.class), prescriptions,
                mock(AppointmentJpaRepository.class));
        when(prescriptions.save(any(Prescription.class))).thenAnswer(call -> call.getArgument(0));
        ConsultationController.PrescriptionRequest request = new ConsultationController.PrescriptionRequest();
        request.prescription = new Prescription.Builder()
                .setPatientId("PAT-1")
                .setDateIssued(LocalDate.now())
                .setExpiryDate(LocalDate.now().plusDays(30))
                .build();
        request.items = List.of();

        Prescription saved = controller.createPrescription(request, doctorUser("DOC-AUTHENTICATED")).getBody();

        assertTrue(saved.getPrescriptionId().startsWith("RX-"));
        assertEquals("DOC-AUTHENTICATED", saved.getDoctorId());
    }

    @Test
    void rejectsMissingPrescriptionBodyAndBlankPatientId() {
        ConsultationController controller = controller(mock(MedicalRecordJpaRepository.class),
                mock(PrescriptionJpaRepository.class), mock(AppointmentJpaRepository.class));

        assertEquals("Prescription body is required",
                assertThrows(IllegalArgumentException.class,
                        () -> controller.createPrescription(null, doctorUser("DOC-1"))).getMessage());

        ConsultationController.PrescriptionRequest request = new ConsultationController.PrescriptionRequest();
        request.prescription = new Prescription.Builder().setPatientId(" ").build();
        assertEquals("Patient ID is required",
                assertThrows(IllegalArgumentException.class,
                        () -> controller.createPrescription(request, doctorUser("DOC-1"))).getMessage());
    }

    @Test
    void rejectsAppointmentLinkedToAnotherDoctorOrPatient() {
        AppointmentJpaRepository appointments = mock(AppointmentJpaRepository.class);
        ConsultationController controller = controller(mock(MedicalRecordJpaRepository.class),
                mock(PrescriptionJpaRepository.class), appointments);
        Appointment appointment = new Appointment.Builder()
                .setAppointmentId("APT-1")
                .setPatientId("PAT-1")
                .setDoctorId("DOC-OTHER")
                .setAppointmentDate(LocalDateTime.now())
                .setStatus(AppointmentStatus.CONFIRMED)
                .build();
        when(appointments.findById("APT-1")).thenReturn(Optional.of(appointment));
        MedicalRecord request = new MedicalRecord.Builder()
                .setRecordId("MR-1")
                .setPatientId("PAT-1")
                .setDoctorId("DOC-AUTHENTICATED")
                .setAppointmentId("APT-1")
                .build();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.createMedicalRecord(request, doctorUser("DOC-AUTHENTICATED")));
    }

    private ConsultationController controller(MedicalRecordJpaRepository records,
                                              PrescriptionJpaRepository prescriptions,
                                              AppointmentJpaRepository appointments) {
        ConsultationService consultationService = new ConsultationService(
                records, prescriptions, mock(PrescriptionItemJpaRepository.class),
                patientRepository());
        AppointmentService appointmentService = new AppointmentService(
                appointments, mock(DoctorJpaRepository.class), mock(PatientJpaRepository.class));
        return new ConsultationController(consultationService, appointmentService);
    }

    private PatientJpaRepository patientRepository() {
        PatientJpaRepository patients = mock(PatientJpaRepository.class);
        when(patients.existsById("PAT-1")).thenReturn(true);
        return patients;
    }

    private void setRecordField(MedicalRecord record, String fieldName, String value)
            throws ReflectiveOperationException {
        java.lang.reflect.Field field = MedicalRecord.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(record, value);
    }

    private User doctorUser(String doctorId) {
        return new User.Builder()
                .setUsername("doctor")
                .setLinkedProfileId(doctorId)
                .setRoles(Set.of(new Role.Builder().setName("ROLE_DOCTOR").build()))
                .build();
    }
}
