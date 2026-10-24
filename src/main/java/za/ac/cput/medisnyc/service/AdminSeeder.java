package za.ac.cput.medisnyc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.medisnyc.domain.Role;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.repository.jpa.RoleJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.UserJpaRepository;

import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.seed.admin", havingValue = "true", matchIfMissing = true)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserJpaRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public AdminSeeder(UserJpaRepository userRepository, RoleJpaRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.seed.admin-username:admin}") String username,
                       @Value("${app.seed.admin-email:admin@medisync.example}") String email,
                       @Value("${app.seed.admin-password:Admin@12345}") String password) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername(username)) {
            logger.info("Admin seed skipped; username already exists");
            return;
        }
        if (password.length() < 8) {
            throw new IllegalStateException("app.seed.admin-password must be at least 8 characters");
        }
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role.Builder().setName("ROLE_ADMIN").build()));
        User admin = new User.Builder()
                .setUsername(username)
                .setEmail(email)
                .setPassword(passwordEncoder.encode(password))
                .setFirstName("System")
                .setLastName("Administrator")
                .setEnabled(true)
                .setRoles(Set.of(adminRole))
                .build();
        userRepository.save(admin);
        logger.info("Created bootstrap admin account for username {}", username);
    }
}
