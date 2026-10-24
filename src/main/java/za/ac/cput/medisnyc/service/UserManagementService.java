package za.ac.cput.medisnyc.service;

/* UserManagementService.java
   Module 6: Reports & Administration - User Management (admin only).
*/

import za.ac.cput.medisnyc.domain.Role;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.repository.jpa.AppointmentJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.MedicalRecordJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.OrderJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PatientJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.RoleJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.UserJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;
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
    private final PatientJpaRepository patientRepository;
    private final AppointmentJpaRepository appointmentRepository;
    private final OrderJpaRepository orderRepository;
    private final MedicalRecordJpaRepository medicalRecordRepository;

    @Autowired
    public UserManagementService(UserJpaRepository userRepository, RoleJpaRepository roleRepository,
                                 AuditLogService auditLogService, DoctorJpaRepository doctorRepository,
                                 PatientService patientService, PasswordEncoder passwordEncoder,
                                 PatientJpaRepository patientRepository,
                                 AppointmentJpaRepository appointmentRepository,
                                 OrderJpaRepository orderRepository,
                                 MedicalRecordJpaRepository medicalRecordRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.doctorRepository = doctorRepository;
        this.patientService = patientService;
        this.passwordEncoder = passwordEncoder;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.orderRepository = orderRepository;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Transactional
    public User setEnabled(Long userId, boolean enabled, User actingAdmin) {
        User existing = getUser(userId);
        if (!enabled && sameUser(existing, actingAdmin)) {
            throw new IllegalStateException("You cannot disable your own admin account");
        }
        if (!enabled && existing.isEnabled() && isAdmin(existing)) {
            requireAnotherEnabledAdmin(existing);
        }
        User updated = new User.Builder().copy(existing).setEnabled(enabled).build();
        User saved = userRepository.save(updated);
        auditLogService.log(actingAdmin.getUsername(), enabled ? "ENABLE_USER" : "DISABLE_USER",
                target(existing), "enabled: " + existing.isEnabled() + " -> " + saved.isEnabled());
        return saved;
    }

    @Transactional
    public User assignRole(Long userId, String roleName, User actingAdmin) {
        User existing = getUser(userId);
        if (sameUser(existing, actingAdmin)) {
            throw new IllegalStateException("You cannot change your own admin role");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        String normalized = roleName.toUpperCase().startsWith("ROLE_")
                ? roleName.toUpperCase() : "ROLE_" + roleName.toUpperCase();
        if (!List.of("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PHARMACIST", "ROLE_PATIENT").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported role: " + roleName);
        }
        Role role = roleRepository.findByName(normalized)
                .orElseGet(() -> roleRepository.save(new Role.Builder().setName(normalized).build()));
        if (isAdmin(existing) && !normalized.equals("ROLE_ADMIN")) {
            requireAnotherEnabledAdmin(existing);
        }
        // Role assignment replaces the complete role set; it does not add a role.
        User updated = new User.Builder().copy(existing).setRoles(java.util.Set.of(role)).build();
        User saved = userRepository.save(updated);
        auditLogService.log(actingAdmin.getUsername(), "ASSIGN_ROLE", target(existing),
                "roles: " + roleNames(existing) + " -> " + roleNames(saved));
        return saved;
    }

    @Transactional
    public User linkProfile(Long userId, String linkedProfileId, User actingAdmin) {
        User existing = getUser(userId);
        validateProfile(existing, linkedProfileId);
        User updated = new User.Builder().copy(existing).setLinkedProfileId(linkedProfileId).build();
        User saved = userRepository.save(updated);
        auditLogService.log(actingAdmin.getUsername(), "LINK_PROFILE", target(existing),
                "linkedProfileId: " + existing.getLinkedProfileId() + " -> " + saved.getLinkedProfileId());
        return saved;
    }

    @Transactional
    public User linkNewPatient(Long userId, String phoneNumber, java.time.LocalDate dateOfBirth, User actingAdmin) {
        User existing = getUser(userId);
        if (!hasRole(existing, "ROLE_PATIENT")) {
            throw new IllegalArgumentException("Only patient accounts can be linked to a patient profile");
        }
        za.ac.cput.medisnyc.domain.Patient patient = patientService.create(existing.getFirstName(),
                existing.getLastName(), existing.getEmail(), phoneNumber, dateOfBirth, List.of());
        User saved = saveLinkedProfile(existing, patient.getMedicalId());
        auditLogService.log(actingAdmin.getUsername(), "LINK_PATIENT", target(existing),
                "linkedProfileId: " + existing.getLinkedProfileId() + " -> " + saved.getLinkedProfileId());
        return saved;
    }

    @Transactional
    public User linkDoctor(Long userId, String doctorId, User actingAdmin) {
        User existing = getUser(userId);
        if (!hasRole(existing, "ROLE_DOCTOR")) {
            throw new IllegalArgumentException("Only doctor accounts can be linked to a doctor profile");
        }
        validateProfile(existing, doctorId);
        User saved = saveLinkedProfile(existing, doctorId);
        auditLogService.log(actingAdmin.getUsername(), "LINK_DOCTOR", target(existing),
                "linkedProfileId: " + existing.getLinkedProfileId() + " -> " + saved.getLinkedProfileId());
        return saved;
    }

    @Transactional
    @Override
    public User createDoctorLogin(String doctorId, String username, String email, String password) {
        return createDoctorLogin(doctorId, username, email, password, null);
    }

    @Transactional
    public User createDoctorLogin(String doctorId, String username, String email, String password, User actingAdmin) {
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
        auditLogService.log(actingAdmin == null ? "SYSTEM" : actingAdmin.getUsername(),
                "CREATE_DOCTOR_LOGIN", target(saved),
                "user created: <none> -> username=" + saved.getUsername() + ", roles="
                        + roleNames(saved) + ", enabled=" + saved.isEnabled()
                        + ", linkedProfileId=" + saved.getLinkedProfileId());
        return saved;
    }

    @Transactional
    public User deleteUser(Long userId, boolean hard, User actingAdmin) {
        User existing = getUser(userId);
        if (sameUser(existing, actingAdmin)) {
            throw new IllegalStateException("You cannot delete your own admin account");
        }
        if (existing.isEnabled() && isAdmin(existing)) {
            requireAnotherEnabledAdmin(existing);
        }
        if (hard) {
            if (hasLinkedActivity(existing)) {
                throw new IllegalStateException("User cannot be hard-deleted while linked appointments, orders, or records exist");
            }
            auditLogService.log(actingAdmin.getUsername(), "DELETE_USER", target(existing),
                    "user: " + userSnapshot(existing) + " -> deleted");
            userRepository.delete(existing);
            return existing;
        }
        User disabled = new User.Builder().copy(existing).setEnabled(false).build();
        User saved = userRepository.save(disabled);
        auditLogService.log(actingAdmin.getUsername(), "DISABLE_USER", target(existing),
                "user: " + userSnapshot(existing) + " -> " + userSnapshot(saved));
        return saved;
    }

    private User saveLinkedProfile(User existing, String linkedProfileId) {
        return userRepository.save(new User.Builder().copy(existing)
                .setLinkedProfileId(linkedProfileId).build());
    }

    private void validateProfile(User user, String linkedProfileId) {
        if (linkedProfileId == null || linkedProfileId.isBlank()) {
            throw new IllegalArgumentException("Linked profile ID is required");
        }
        if (hasRole(user, "ROLE_PATIENT")) {
            if (!patientRepository.existsById(linkedProfileId)) {
                throw new IllegalArgumentException("Patient not found: " + linkedProfileId);
            }
            return;
        }
        if (hasRole(user, "ROLE_DOCTOR")) {
            if (!doctorRepository.existsById(linkedProfileId)) {
                throw new IllegalArgumentException("Doctor not found: " + linkedProfileId);
            }
            return;
        }
        throw new IllegalArgumentException("Only patient or doctor accounts can be linked to a profile");
    }

    private void requireAnotherEnabledAdmin(User target) {
        boolean anotherEnabledAdmin = userRepository.findAll().stream()
                .anyMatch(user -> !user.getUserId().equals(target.getUserId())
                        && user.isEnabled() && isAdmin(user));
        if (!anotherEnabledAdmin) {
            throw new IllegalStateException("The last enabled admin account cannot be disabled or lose its admin role");
        }
    }

    private boolean hasLinkedActivity(User user) {
        String profileId = user.getLinkedProfileId();
        if (profileId == null || profileId.isBlank()) return false;
        return appointmentRepository.existsByPatientId(profileId)
                || appointmentRepository.existsByDoctorId(profileId)
                || orderRepository.existsByPatientId(profileId)
                || medicalRecordRepository.existsByPatientId(profileId)
                || medicalRecordRepository.existsByDoctorId(profileId);
    }

    private boolean sameUser(User first, User second) {
        return second != null && first.getUserId().equals(second.getUserId());
    }

    private boolean isAdmin(User user) {
        return hasRole(user, "ROLE_ADMIN");
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private String roleNames(User user) {
        return user.getRoles().stream().map(Role::getName).sorted().toList().toString();
    }

    private String target(User user) {
        return String.valueOf(user.getUserId());
    }

    private String userSnapshot(User user) {
        return "enabled=" + user.isEnabled() + ", roles=" + roleNames(user)
                + ", linkedProfileId=" + user.getLinkedProfileId();
    }
}
