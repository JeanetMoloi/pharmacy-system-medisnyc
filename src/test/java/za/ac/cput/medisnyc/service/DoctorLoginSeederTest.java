package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import za.ac.cput.medisnyc.domain.Doctor;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.UserJpaRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorLoginSeederTest {

    @Test
    void generatesUsernameWithDuplicateSuffixAndNormalizedNames() {
        DoctorJpaRepository doctors = mock(DoctorJpaRepository.class);
        UserJpaRepository users = mock(UserJpaRepository.class);
        DoctorLoginCreator creator = mock(DoctorLoginCreator.class);
        Set<String> usernames = Set.of("andile.mthembu");
        when(users.existsByUsername(anyString())).thenAnswer(call ->
                usernames.contains(call.getArgument(0)));

        DoctorLoginSeeder seeder = new DoctorLoginSeeder(doctors, users, creator, "password");
        assertEquals("andile.mthembu2", seeder.uniqueUsername(doctor(
                "1", "Andile", "Mthembu")));
        assertEquals("jose.oconnorsmith", seeder.uniqueUsername(doctor(
                "2", "José", "O'Connor-Smith")));
        assertEquals("ana.muller", seeder.uniqueUsername(doctor(
                "3", "Dr. Ana", "Müller")));
    }

    @Test
    void seedingTwiceDoesNotCreateAdditionalLinkedAccounts() {
        DoctorJpaRepository doctors = mock(DoctorJpaRepository.class);
        UserJpaRepository users = mock(UserJpaRepository.class);
        DoctorLoginCreator creator = mock(DoctorLoginCreator.class);
        List<Doctor> doctorList = List.of(
                doctor("DOC-1", "Andile", "Mthembu"),
                doctor("DOC-2", "Rashida", "Moosa"));
        Set<String> linkedProfiles = new HashSet<>();

        when(doctors.findAll()).thenReturn(doctorList);
        when(users.existsByLinkedProfileId(anyString()))
                .thenAnswer(call -> linkedProfiles.contains(call.getArgument(0)));
        when(users.existsByUsername(anyString())).thenReturn(false);
        doAnswer(call -> {
            linkedProfiles.add(call.getArgument(0));
            return null;
        }).when(creator).createDoctorLogin(anyString(), anyString(), anyString(), anyString());

        DoctorLoginSeeder seeder = new DoctorLoginSeeder(doctors, users, creator, "password");
        seeder.run();
        seeder.run();

        verify(creator, org.mockito.Mockito.times(2))
                .createDoctorLogin(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void doesNotRegisterWhenDisabledByConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(DoctorLoginSeeder.class)
                .withPropertyValues(
                        "app.seed.doctor-logins=false",
                        "app.seed.doctor-password=password")
                .run(context -> {
                    assertFalse(context.containsBean("doctorLoginSeeder"));
                });
    }

    private static Doctor doctor(String id, String firstName, String lastName) {
        return new Doctor.Builder()
                .setDoctorId(id)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setSpecialization("General Practitioner")
                .setPhoneNumber("+27821234001")
                .setEmail(id.toLowerCase() + "@medisync.example")
                .build();
    }
}
