package za.ac.cput.medisnyc.repository.jpa;

import za.ac.cput.medisnyc.domain.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalRecordJpaRepository extends JpaRepository<MedicalRecord, String> {
    List<MedicalRecord> findByPatientId(String patientId);
    List<MedicalRecord> findByDoctorId(String doctorId);
    boolean existsByPatientId(String patientId);
    boolean existsByDoctorId(String doctorId);
}