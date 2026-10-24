package za.ac.cput.medisnyc.controller;

/* AdminController.java
   Module 6: Reports & Administration - User Management API / Audit Log.
   Author: Phemelo
*/

import za.ac.cput.medisnyc.domain.AuditLog;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.dto.UserResponse;
import za.ac.cput.medisnyc.service.AuditLogService;
import za.ac.cput.medisnyc.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserManagementService userManagementService;
    private final AuditLogService auditLogService;

    @Autowired
    public AdminController(UserManagementService userManagementService, AuditLogService auditLogService) {
        this.userManagementService = userManagementService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userManagementService.getAllUsers().stream().map(UserResponse::from).toList());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(UserResponse.from(userManagementService.getUser(userId)));
    }

    @PutMapping("/users/{userId}/enabled")
    public ResponseEntity<UserResponse> setEnabled(@PathVariable Long userId, @RequestParam boolean enabled,
                                                    @AuthenticationPrincipal User actingAdmin) {
        return ResponseEntity.ok(UserResponse.from(userManagementService.setEnabled(userId, enabled, actingAdmin)));
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<UserResponse> assignRole(@PathVariable Long userId, @RequestParam String role,
                                                    @AuthenticationPrincipal User actingAdmin) {
        return ResponseEntity.ok(UserResponse.from(userManagementService.assignRole(userId, role, actingAdmin)));
    }

    @PutMapping("/users/{userId}/linked-profile")
    public ResponseEntity<UserResponse> setLinkedProfile(@PathVariable Long userId,
                                                         @RequestParam String linkedProfileId,
                                                         @AuthenticationPrincipal User actingAdmin) {
        return ResponseEntity.ok(UserResponse.from(
                userManagementService.linkProfile(userId, linkedProfileId, actingAdmin)));
    }

    @PutMapping("/users/{userId}/link-patient")
    public ResponseEntity<UserResponse> linkPatient(@PathVariable Long userId,
                                                    @Valid @RequestBody LinkPatientRequest request,
                                                    @AuthenticationPrincipal User actingAdmin) {
        return ResponseEntity.ok(UserResponse.from(userManagementService.linkNewPatient(userId,
                request.phoneNumber, request.dateOfBirth, actingAdmin)));
    }

    @PutMapping("/users/{userId}/link-doctor")
    public ResponseEntity<UserResponse> linkDoctor(@PathVariable Long userId, @RequestParam String doctorId,
                                                   @AuthenticationPrincipal User actingAdmin) {
        return ResponseEntity.ok(UserResponse.from(userManagementService.linkDoctor(userId, doctorId, actingAdmin)));
    }

    @PostMapping("/users/doctor-login")
    public ResponseEntity<UserResponse> createDoctorLogin(@Valid @RequestBody CreateDoctorLoginRequest request,
                                                          @AuthenticationPrincipal User actingAdmin) {
        return ResponseEntity.status(201).body(UserResponse.from(userManagementService.createDoctorLogin(
                request.doctorId, request.username, request.email, request.password, actingAdmin)));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "false") boolean hard,
                                        @AuthenticationPrincipal User actingAdmin) {
        User affected = userManagementService.deleteUser(userId, hard, actingAdmin);
        return hard ? ResponseEntity.noContent().build() : ResponseEntity.ok(UserResponse.from(affected));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<AuditLogPageResponse> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action) {
        Page<AuditLog> result = auditLogService.getPage(page, size, username, action);
        return ResponseEntity.ok(new AuditLogPageResponse(result.getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    public record AuditLogPageResponse(List<AuditLog> content, int page, int size,
                                       long totalElements, int totalPages) {}

    public static class LinkPatientRequest {
        @NotBlank
        public String phoneNumber;
        @jakarta.validation.constraints.NotNull
        public LocalDate dateOfBirth;
    }

    public static class CreateDoctorLoginRequest {
        @NotBlank public String doctorId;
        @NotBlank public String username;
        @NotBlank @Email public String email;
        @NotBlank @Size(min = 8) public String password;
    }
}