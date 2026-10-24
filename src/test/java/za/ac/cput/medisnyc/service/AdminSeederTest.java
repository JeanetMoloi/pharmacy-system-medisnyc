package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import za.ac.cput.medisnyc.domain.Role;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.repository.jpa.RoleJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.UserJpaRepository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSeederTest {

    @Test
    void seedsAdminOnlyOnceAndEncodesPassword() {
        UserJpaRepository users = mock(UserJpaRepository.class);
        RoleJpaRepository roles = mock(RoleJpaRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Role adminRole = new Role.Builder().setName("ROLE_ADMIN").build();
        AtomicReference<User> saved = new AtomicReference<>();
        when(users.existsByUsername("admin")).thenReturn(false, true);
        when(roles.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(users.save(any(User.class))).thenAnswer(call -> {
            saved.set(call.getArgument(0));
            return saved.get();
        });
        AdminSeeder seeder = new AdminSeeder(users, roles, encoder,
                "admin", "admin@medisync.example", "Admin@12345");

        seeder.run();
        seeder.run();

        verify(users, times(1)).save(any(User.class));
        assertEquals("ROLE_ADMIN", saved.get().getRoles().iterator().next().getName());
        assertTrue(encoder.matches("Admin@12345", saved.get().getPassword()));
        assertNotEquals("Admin@12345", saved.get().getPassword());
    }
}
