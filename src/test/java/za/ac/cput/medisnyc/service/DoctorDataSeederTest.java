package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import za.ac.cput.medisnyc.domain.Doctor;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;
import za.ac.cput.medisnyc.util.SpecializationCatalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorDataSeederTest {

    @Test
    void runningTwiceDoesNotAddDuplicateDoctors() {
        DoctorJpaRepository repository = mock(DoctorJpaRepository.class);
        Set<String> storedEmails = new HashSet<>();

        when(repository.existsByEmailIgnoreCase(anyString()))
                .thenAnswer(invocation -> storedEmails.contains(invocation.getArgument(0)));
        doAnswer(invocation -> {
            Doctor doctor = invocation.getArgument(0);
            storedEmails.add(doctor.getEmail());
            return doctor;
        }).when(repository).save(any(Doctor.class));

        DoctorService service = new DoctorService(repository);
        DoctorDataSeeder seeder = new DoctorDataSeeder(repository, service);
        seeder.run();
        seeder.run();

        verify(repository, times(16)).save(any(Doctor.class));
    }

    @Test
    void seedsAtLeastOneDoctorForEveryCatalogSpecialty() {
        DoctorJpaRepository repository = mock(DoctorJpaRepository.class);
        List<Doctor> seededDoctors = new ArrayList<>();

        doAnswer(invocation -> {
            Doctor doctor = invocation.getArgument(0);
            seededDoctors.add(doctor);
            return doctor;
        }).when(repository).save(any(Doctor.class));

        new DoctorDataSeeder(repository, new DoctorService(repository)).run();

        Set<String> seededSpecializations = seededDoctors.stream()
                .map(Doctor::getSpecialization)
                .collect(Collectors.toSet());
        assertTrue(seededSpecializations.containsAll(SpecializationCatalog.getSpecializations()));
    }
}
