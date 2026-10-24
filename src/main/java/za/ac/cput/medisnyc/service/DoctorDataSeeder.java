package za.ac.cput.medisnyc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.sample-doctors", havingValue = "true", matchIfMissing = true)
@Order(1)
public class DoctorDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DoctorDataSeeder.class);
    private static final List<SampleDoctor> SAMPLE_DOCTORS = List.of(
            new SampleDoctor("Thandi", "Nkosi", "General Practitioner", "+27821234001", "thandi.nkosi@medisync.example"),
            new SampleDoctor("Pieter", "van der Merwe", "General Practitioner", "+27211234002", "pieter.vandermerwe@medisync.example"),
            new SampleDoctor("Ayesha", "Patel", "Pediatrician", "+27821234003", "ayesha.patel@medisync.example"),
            new SampleDoctor("Lisakhanya", "Mpahla", "Dentist", "+27821234004", "lisakhanya.mpahla@medisync.example"),
            new SampleDoctor("Sipho", "Dlamini", "Dentist", "+27821234005", "sipho.dlamini@medisync.example"),
            new SampleDoctor("Johan", "Botha", "Cardiologist", "+27211234006", "johan.botha@medisync.example"),
            new SampleDoctor("Naledi", "Mokoena", "Dermatologist", "+27821234007", "naledi.mokoena@medisync.example"),
            new SampleDoctor("Zanele", "Khumalo", "Gynecologist", "+27821234008", "zanele.khumalo@medisync.example"),
            new SampleDoctor("Mohamed", "Cassim", "Orthopedic Surgeon", "+27211234009", "mohamed.cassim@medisync.example"),
            new SampleDoctor("Karen", "Smith", "ENT Specialist", "+27211234010", "karen.smith@medisync.example"),
            new SampleDoctor("Themba", "Ndlovu", "Ophthalmologist", "+27821234011", "themba.ndlovu@medisync.example"),
            new SampleDoctor("Fatima", "Hendricks", "Neurologist", "+27211234012", "fatima.hendricks@medisync.example"),
            new SampleDoctor("Michael", "Naidoo", "Psychiatrist", "+27821234013", "michael.naidoo@medisync.example"),
            new SampleDoctor("Busisiwe", "Zulu", "Endocrinologist", "+27821234014", "busisiwe.zulu@medisync.example"),
            new SampleDoctor("Andile", "Mthembu", "Urologist", "+27821234015", "andile.mthembu@medisync.example"),
            new SampleDoctor("Rashida", "Moosa", "Physician (Internal Medicine)", "+27211234016", "rashida.moosa@medisync.example")
    );

    private final DoctorJpaRepository doctorRepository;
    private final DoctorService doctorService;

    public DoctorDataSeeder(DoctorJpaRepository doctorRepository, DoctorService doctorService) {
        this.doctorRepository = doctorRepository;
        this.doctorService = doctorService;
    }

    @Override
    public void run(String... args) {
        int added = 0;
        for (SampleDoctor sample : SAMPLE_DOCTORS) {
            if (!doctorRepository.existsByEmailIgnoreCase(sample.email())) {
                doctorService.create(sample.firstName(), sample.lastName(), sample.specialization(),
                        sample.phoneNumber(), sample.email());
                added++;
            }
        }
        logger.info("Added {} sample doctors", added);
    }

    private record SampleDoctor(String firstName, String lastName, String specialization,
                                String phoneNumber, String email) {
    }
}
