package za.ac.cput.medisnyc.controller;

/* AppointmentController.java
   Module 2: Patient & Appointment Module.
   Author: Lisakhanya Mpahla
*/

import za.ac.cput.medisnyc.domain.Appointment;
import za.ac.cput.medisnyc.domain.AppointmentStatus;
import za.ac.cput.medisnyc.dto.CreateAppointmentRequest;
import za.ac.cput.medisnyc.service.AppointmentService;
import za.ac.cput.medisnyc.domain.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<Appointment> bookAppointment(@Valid @RequestBody CreateAppointmentRequest request,
                                                        @AuthenticationPrincipal User user,
                                                        Authentication authentication) {
        if (hasRole(authentication, "PATIENT")) {
            if (user.getLinkedProfileId() == null) {
                throw new AccessDeniedException("Patient account has no linked patient record");
            }
            request.setPatientId(user.getLinkedProfileId());
        }
        Appointment created = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getByPatient(@PathVariable String patientId,
                                                           @AuthenticationPrincipal User user,
                                                           Authentication authentication) {
        if (hasRole(authentication, "PATIENT") && !patientId.equals(user.getLinkedProfileId())) {
            throw new AccessDeniedException("Patients may only view their own appointments");
        }
        return ResponseEntity.ok(appointmentService.getByPatient(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getByDoctor(@PathVariable String doctorId,
                                                          @AuthenticationPrincipal User user,
                                                          Authentication authentication) {
        if (hasRole(authentication, "DOCTOR") && !doctorId.equals(user.getLinkedProfileId())) {
            throw new AccessDeniedException("Doctors may only view their own appointments");
        }
        return ResponseEntity.ok(appointmentService.getByDoctor(doctorId));
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> getAll() {
        return ResponseEntity.ok(appointmentService.getAll());
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<Appointment> getById(@PathVariable String appointmentId,
                                                @AuthenticationPrincipal User user,
                                                Authentication authentication) {
        Appointment appointment = appointmentService.getById(appointmentId);
        if (hasRole(authentication, "PATIENT") && !appointment.getPatientId().equals(user.getLinkedProfileId())) {
            throw new AccessDeniedException("Patients may only view their own appointments");
        }
        if (hasRole(authentication, "DOCTOR") && !appointment.getDoctorId().equals(user.getLinkedProfileId())) {
            throw new AccessDeniedException("Doctors may only view their own appointments");
        }
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/{appointmentId}/status")
    public ResponseEntity<Appointment> updateStatus(@PathVariable String appointmentId,
                                                    @RequestParam AppointmentStatus status,
                                                    @AuthenticationPrincipal User user,
                                                    Authentication authentication) {
        if (hasRole(authentication, "DOCTOR")) {
            Appointment appointment = appointmentService.getById(appointmentId);
            if (!appointment.getDoctorId().equals(user.getLinkedProfileId())) {
                throw new AccessDeniedException("Doctors may only update their own appointments");
            }
        }
        return ResponseEntity.ok(appointmentService.updateStatus(appointmentId, status));
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(@PathVariable String appointmentId,
                                                          @AuthenticationPrincipal User user,
                                                          Authentication authentication) {
        if (hasRole(authentication, "PATIENT")) {
            Appointment appointment = appointmentService.getById(appointmentId);
            if (!appointment.getPatientId().equals(user.getLinkedProfileId())) {
                throw new AccessDeniedException("Patients may only cancel their own appointments");
            }
        }
        return ResponseEntity.ok(appointmentService.cancelAppointment(appointmentId));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
