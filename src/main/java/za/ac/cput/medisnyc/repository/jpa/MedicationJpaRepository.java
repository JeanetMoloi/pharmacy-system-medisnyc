package za.ac.cput.medisnyc.repository.jpa;

import za.ac.cput.medisnyc.domain.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MedicationJpaRepository extends JpaRepository<Medication, String> {
    List<Medication> findByCategoryIgnoreCase(String category);
    List<Medication> findByMedicationNameContainingIgnoreCase(String name);

    @Query("""
            select count(m) > 0 from Medication m
            where lower(m.medicationName) = lower(:name)
              and lower(m.strength) = lower(:strength)
              and lower(m.dosageForm) = lower(:dosageForm)
              and (:excludedId is null or m.medicationId <> :excludedId)
            """)
    boolean existsDuplicate(@Param("name") String name, @Param("strength") String strength,
                            @Param("dosageForm") String dosageForm, @Param("excludedId") String excludedId);
}