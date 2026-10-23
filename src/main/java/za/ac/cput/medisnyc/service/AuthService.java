package za.ac.cput.medisnyc.service;

/* AuthService.java
   Module 1: registration, login, forgot/reset password.
   Author: Siphesihle
*/

import za.ac.cput.medisnyc.domain.PasswordResetToken;
import za.ac.cput.medisnyc.domain.Role;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.domain.Patient;
import za.ac.cput.medisnyc.dto.AuthResponse;
import za.ac.cput.medisnyc.dto.LoginRequest;
import za.ac.cput.medisnyc.dto.RegisterRequest;
import za.ac.cput.medisnyc.repository.jpa.PasswordResetTokenJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.RoleJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.UserJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PatientJpaRepository;
import za.ac.cput.medisnyc.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserJpaRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final PasswordResetTokenJpaRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PatientJpaRepository patientRepository;
    private final AuditLogService auditLogService;
    private final PasswordResetEmailService passwordResetEmailService;

    @Autowired
    public AuthService(UserJpaRepository userRepository,
                       RoleJpaRepository roleRepository,
                       PasswordResetTokenJpaRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       PatientJpaRepository patientRepository,
                       AuditLogService auditLogService,
                       PasswordResetEmailService passwordResetEmailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.patientRepository = patientRepository;
        this.auditLogService = auditLogService;
        this.passwordResetEmailService = passwordResetEmailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!"PATIENT".equalsIgnoreCase(request.getRole())) {
            throw new IllegalArgumentException("Only patient accounts can be self-registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        if (patientRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered to a patient");
        }

        Patient patient = new Patient.Builder()
                .setMedicalId(za.ac.cput.medisnyc.util.Helper.generateId("PAT"))
                .setFirstName(request.getFirstName())
                .setLastName(request.getLastName())
                .setEmail(request.getEmail())
                .setPhoneNumber(request.getPhoneNumber())
                .setDateOfBirth(request.getDateOfBirth())
                .build();
        Patient savedPatient = patientRepository.save(patient);

        String roleName = "ROLE_PATIENT";
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role.Builder().setName(roleName).build()));

        User user = new User.Builder()
                .setUsername(request.getUsername())
                .setEmail(request.getEmail())
                .setPassword(passwordEncoder.encode(request.getPassword()))
                .setFirstName(request.getFirstName())
                .setLastName(request.getLastName())
                .setLinkedProfileId(savedPatient.getMedicalId())
                .setEnabled(true)
                .addRole(role)
                .build();

        User saved = userRepository.save(user);
        auditLogService.log(saved.getUsername(), "REGISTER", saved.getUsername(), "Patient account registered");
        String token = jwtUtil.generateToken(saved);

        List<String> roles = saved.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList());
        return new AuthResponse(token, saved.getUsername(), saved.getEmail(), roles);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            auditLogService.log(request.getUsername(), "LOGIN_FAILED", request.getUsername(),
                    "Authentication failed");
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        auditLogService.log(user.getUsername(), "LOGIN", user.getUsername(), "User signed in");
        String token = jwtUtil.generateToken(user);
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList());
        return new AuthResponse(token, user.getUsername(), user.getEmail(), roles);
    }

    @Transactional
    public String createPasswordResetToken(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return null;
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken.Builder()
                .setToken(token)
                .setUsername(user.getUsername())
                .setExpiryDate(LocalDateTime.now().plusHours(1))
                .setUsed(false)
                .build();
        resetTokenRepository.save(resetToken);
        passwordResetEmailService.send(user.getEmail(), token);

        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        User user = userRepository.findByUsername(resetToken.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User no longer exists"));

        User updated = new User.Builder().copy(user)
                .setPassword(passwordEncoder.encode(newPassword))
                .build();
        userRepository.save(updated);

        PasswordResetToken usedToken = new PasswordResetToken.Builder()
                .setId(resetToken.getId())
                .setToken(resetToken.getToken())
                .setUsername(resetToken.getUsername())
                .setExpiryDate(resetToken.getExpiryDate())
                .setUsed(true)
                .build();
        resetTokenRepository.save(usedToken);
    }
}