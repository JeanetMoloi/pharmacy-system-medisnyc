package za.ac.cput.medisnyc.controller;

import jakarta.validation.Valid;
import za.ac.cput.medisnyc.domain.Patient;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<List<Patient>> getAll() {
        return ResponseEntity.ok(patientService.getAll());
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<Patient> getById(@PathVariable String patientId,
                                           @AuthenticationPrincipal User user,
                                           Authentication authentication) {
        if (hasRole(authentication, "PATIENT")
                && (user.getLinkedProfileId() == null || !user.getLinkedProfileId().equals(patientId))) {
            throw new AccessDeniedException("Patients may only view their own record");
        }
        return ResponseEntity.ok(patientService.getById(patientId));
    }

    @PostMapping
    public ResponseEntity<Patient> create(@Valid @RequestBody CreatePatientRequest request) {
        Patient created = patientService.create(request.firstName, request.lastName, request.email,
                request.phoneNumber, request.dateOfBirth, request.allergies);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + role));
    }

    public static class CreatePatientRequest {
        public String firstName;
        public String lastName;
        public String email;
        public String phoneNumber;
        public LocalDate dateOfBirth;
        public List<String> allergies = List.of();
    }
}
