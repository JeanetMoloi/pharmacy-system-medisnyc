package za.ac.cput.medisnyc.service;

/* UserManagementService.java
   Module 6: Reports & Administration - User Management (admin only).
*/

import za.ac.cput.medisnyc.domain.Role;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.repository.jpa.RoleJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.UserJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;
import za.ac.cput.medisnyc.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserManagementService implements DoctorLoginCreator {

    private final UserJpaRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final AuditLogService auditLogService;
    private final DoctorJpaRepository doctorRepository;
    private final PatientService patientService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserManagementService(UserJpaRepository userRepository, RoleJpaRepository roleRepository,
                                 AuditLogService auditLogService, DoctorJpaRepository doctorRepository,
                                 PatientService patientService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.doctorRepository = doctorRepository;
        this.patientService = patientService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Transactional
    public User setEnabled(Long userId, boolean enabled) {
        User existing = getUser(userId);
        User updated = new User.Builder().copy(existing).setEnabled(enabled).build();
        return userRepository.save(updated);
    }

    @Transactional
    public User assignRole(Long userId, String roleName) {
        User existing = getUser(userId);
        String normalized = roleName.toUpperCase().startsWith("ROLE_")
                ? roleName.toUpperCase() : "ROLE_" + roleName.toUpperCase();
        if (!List.of("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PHARMACIST", "ROLE_PATIENT").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported role: " + roleName);
        }
        Role role = roleRepository.findByName(normalized)
                .orElseGet(() -> roleRepository.save(new Role.Builder().setName(normalized).build()));
        User updated = new User.Builder().copy(existing).setRoles(java.util.Set.of(role)).build();
        User saved = userRepository.save(updated);
        auditLogService.log(saved.getUsername(), "ASSIGN_ROLE", saved.getUsername(),
                "Role replaced with " + normalized);
        return saved;
    }

    @Transactional
    public User linkProfile(Long userId, String linkedProfileId) {
        User existing = getUser(userId);
        User updated = new User.Builder().copy(existing).setLinkedProfileId(linkedProfileId).build();
        User saved = userRepository.save(updated);
        auditLogService.log(saved.getUsername(), "LINK_PROFILE", saved.getUsername(),
                "Linked profile " + linkedProfileId);
        return saved;
    }

    @Transactional
    public User linkNewPatient(Long userId, String phoneNumber, java.time.LocalDate dateOfBirth) {
        User existing = getUser(userId);
        za.ac.cput.medisnyc.domain.Patient patient = patientService.create(existing.getFirstName(),
                existing.getLastName(), existing.getEmail(), phoneNumber, dateOfBirth, List.of());
        return linkProfile(userId, patient.getMedicalId());
    }

    @Transactional
    public User linkDoctor(Long userId, String doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new IllegalArgumentException("Doctor not found: " + doctorId);
        }
        return linkProfile(userId, doctorId);
    }

    @Transactional
    @Override
    public User createDoctorLogin(String doctorId, String username, String email, String password) {
        za.ac.cput.medisnyc.domain.Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));
        if (userRepository.existsByUsername(username) || userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Username or email is already in use");
        }
        Role role = roleRepository.findByName("ROLE_DOCTOR")
                .orElseGet(() -> roleRepository.save(new Role.Builder().setName("ROLE_DOCTOR").build()));
        User user = new User.Builder()
                .setUsername(username)
                .setEmail(email)
                .setPassword(passwordEncoder.encode(password))
                .setFirstName(doctor.getFirstName())
                .setLastName(doctor.getLastName())
                .setLinkedProfileId(doctorId)
                .setEnabled(true)
                .setRoles(java.util.Set.of(role))
                .build();
        User saved = userRepository.save(user);
        auditLogService.log(saved.getUsername(), "CREATE_DOCTOR_LOGIN", saved.getUsername(),
                "Created doctor login linked to " + doctorId);
        return saved;
    }

    @Transactional
    public void deleteUser(Long userId) {
        getUser(userId);
        userRepository.deleteById(userId);
    }
}
