package za.ac.cput.medisnyc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import za.ac.cput.medisnyc.domain.Doctor;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.UserJpaRepository;

import java.text.Normalizer;
import java.util.Locale;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.doctor-logins", havingValue = "true")
public class DoctorLoginSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DoctorLoginSeeder.class);

    private final DoctorJpaRepository doctorRepository;
    private final UserJpaRepository userRepository;
    private final DoctorLoginCreator doctorLoginCreator;
    private final String password;

    public DoctorLoginSeeder(DoctorJpaRepository doctorRepository,
                             UserJpaRepository userRepository,
                             DoctorLoginCreator doctorLoginCreator,
                             @Value("${app.seed.doctor-password}") String password) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.doctorLoginCreator = doctorLoginCreator;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (password.length() < 8) {
            throw new IllegalStateException("app.seed.doctor-password must be at least 8 characters");
        }

        int created = 0;
        for (Doctor doctor : doctorRepository.findAll()) {
            if (userRepository.existsByLinkedProfileId(doctor.getDoctorId())) {
                continue;
            }
            String username = uniqueUsername(doctor);
            doctorLoginCreator.createDoctorLogin(
                    doctor.getDoctorId(), username, doctor.getEmail(), password);
            created++;
        }

        logger.info("Created {} seeded doctor login accounts", created);
        logger.warn("Seeded doctor logins with the shared demo password. Disable app.seed.doctor-logins in production.");
    }

    String uniqueUsername(Doctor doctor) {
        String baseUsername = normalizeName(doctor.getFirstName()) + "."
                + normalizeName(doctor.getLastName());
        String username = baseUsername;
        int suffix = 2;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix++;
        }
        return username;
    }

    static String normalizeName(String name) {
        String withoutTitle = name.replaceAll("(?i)\\bdr\\.?\\s*", "");
        String normalized = Normalizer.normalize(withoutTitle, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }
}
