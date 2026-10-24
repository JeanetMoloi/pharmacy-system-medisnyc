package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import za.ac.cput.medisnyc.domain.Doctor;
import za.ac.cput.medisnyc.domain.Role;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.repository.jpa.AuditLogJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.AppointmentJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.MedicalRecordJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.OrderJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PatientJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.RoleJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.UserJpaRepository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserManagementServiceDoctorLoginTest {

    @Test
    void storesDoctorNamesAndBcryptEncodedPassword() {
        UserJpaRepository users = mock(UserJpaRepository.class);
        RoleJpaRepository roles = mock(RoleJpaRepository.class);
        DoctorJpaRepository doctors = mock(DoctorJpaRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Doctor doctor = new Doctor.Builder()
                .setDoctorId("DOC-1")
                .setFirstName("Andile")
                .setLastName("Mthembu")
                .build();
        Role doctorRole = new Role.Builder().setName("ROLE_DOCTOR").build();
        AtomicReference<User> persistedUser = new AtomicReference<>();

        when(doctors.findById("DOC-1")).thenReturn(Optional.of(doctor));
        when(users.existsByUsername("andile.mthembu")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("andile@medisync.example")).thenReturn(false);
        when(roles.findByName("ROLE_DOCTOR")).thenReturn(Optional.of(doctorRole));
        when(users.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            persistedUser.set(user);
            return user;
        });

        UserManagementService service = new UserManagementService(
                users,
                roles,
                new AuditLogService(mock(AuditLogJpaRepository.class)),
                doctors,
                new PatientService(mock(PatientJpaRepository.class)),
                encoder,
                mock(PatientJpaRepository.class),
                mock(AppointmentJpaRepository.class),
                mock(OrderJpaRepository.class),
                mock(MedicalRecordJpaRepository.class));
        service.createDoctorLogin("DOC-1", "andile.mthembu", "andile@medisnyc.example", "password");

        User saved = persistedUser.get();
        assertEquals("Andile", saved.getFirstName());
        assertEquals("Mthembu", saved.getLastName());
        assertEquals("DOC-1", saved.getLinkedProfileId());
        assertTrue(saved.isEnabled());
        assertEquals("ROLE_DOCTOR", saved.getRoles().iterator().next().getName());
        assertTrue(encoder.matches("password", saved.getPassword()));
        assertNotEquals("password", saved.getPassword());
    }
}
