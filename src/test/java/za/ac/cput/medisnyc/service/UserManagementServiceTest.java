package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import za.ac.cput.medisnyc.domain.AuditLog;
import za.ac.cput.medisnyc.domain.Role;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.repository.jpa.AppointmentJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.AuditLogJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.MedicalRecordJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.OrderJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PatientJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.RoleJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.UserJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserManagementServiceTest {

    @Test
    void blocksAdminFromDisablingSelf() {
        User admin = user(1L, "admin", true, "ROLE_ADMIN");
        UserJpaRepository users = mock(UserJpaRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        UserManagementService service = service(users, mock(AuditLogJpaRepository.class),
                mock(PatientJpaRepository.class));

        assertEquals("You cannot disable your own admin account", assertThrows(IllegalStateException.class,
                () -> service.setEnabled(1L, false, admin)).getMessage());
        verify(users, never()).save(any(User.class));
    }

    @Test
    void blocksDisablingLastEnabledAdmin() {
        User admin = user(1L, "admin", true, "ROLE_ADMIN");
        User actor = user(2L, "other-admin", true, "ROLE_ADMIN");
        UserJpaRepository users = mock(UserJpaRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(users.findAll()).thenReturn(List.of(admin));
        UserManagementService service = service(users, mock(AuditLogJpaRepository.class),
                mock(PatientJpaRepository.class));

        assertThrows(IllegalStateException.class, () -> service.setEnabled(1L, false, actor));
        verify(users, never()).save(any(User.class));
    }

    @Test
    void auditsActualAdminAsActor() {
        User target = user(10L, "patient", true, "ROLE_PATIENT");
        User actor = user(1L, "acting-admin", true, "ROLE_ADMIN");
        UserJpaRepository users = mock(UserJpaRepository.class);
        when(users.findById(10L)).thenReturn(Optional.of(target));
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        AuditLogJpaRepository logs = mock(AuditLogJpaRepository.class);
        UserManagementService service = service(users, logs, mock(PatientJpaRepository.class));

        service.setEnabled(10L, false, actor);

        org.mockito.ArgumentCaptor<AuditLog> entry = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(logs).save(entry.capture());
        assertEquals("acting-admin", entry.getValue().getUsername());
        assertEquals("10", entry.getValue().getEntityAffected());
        assertEquals("enabled: true -> false", entry.getValue().getDetails());
    }

    @Test
    void rejectsLinkToMissingPatientProfile() {
        User patientUser = user(10L, "patient", true, "ROLE_PATIENT");
        UserJpaRepository users = mock(UserJpaRepository.class);
        when(users.findById(10L)).thenReturn(Optional.of(patientUser));
        PatientJpaRepository patients = mock(PatientJpaRepository.class);
        when(patients.existsById("PAT-MISSING")).thenReturn(false);
        UserManagementService service = service(users, mock(AuditLogJpaRepository.class), patients);

        assertEquals("Patient not found: PAT-MISSING", assertThrows(IllegalArgumentException.class,
                () -> service.linkProfile(10L, "PAT-MISSING", user(1L, "admin", true, "ROLE_ADMIN"))).getMessage());
        verify(users, never()).save(any(User.class));
    }

    private UserManagementService service(UserJpaRepository users, AuditLogJpaRepository logs,
                                          PatientJpaRepository patients) {
        return new UserManagementService(users, mock(RoleJpaRepository.class), new AuditLogService(logs),
                mock(DoctorJpaRepository.class), new PatientService(patients), null, patients,
                mock(AppointmentJpaRepository.class), mock(OrderJpaRepository.class),
                mock(MedicalRecordJpaRepository.class));
    }

    private User user(Long id, String username, boolean enabled, String roleName) {
        return new User.Builder().setUserId(id).setUsername(username).setEmail(username + "@example.test")
                .setEnabled(enabled).setRoles(Set.of(new Role.Builder().setName(roleName).build())).build();
    }
}
