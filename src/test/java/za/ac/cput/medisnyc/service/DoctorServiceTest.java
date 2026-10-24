package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DoctorServiceTest {

    private final DoctorJpaRepository repository = mock(DoctorJpaRepository.class);
    private final DoctorService service = new DoctorService(repository);

    @Test
    void createRejectsSpecializationOutsideCatalog() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("Ari", "Example", "Nurse", "0821234567", "ari@example.com"));

        assertEquals("Please choose a valid specialization", exception.getMessage());
        verifyNoInteractions(repository);
    }
}
