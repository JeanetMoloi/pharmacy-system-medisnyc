package za.ac.cput.medisnyc.repository.jpa;

import za.ac.cput.medisnyc.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.time.LocalDate;

public interface InventoryJpaRepository extends JpaRepository<Inventory, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.inventoryId = :inventoryId")
    java.util.Optional<Inventory> findByIdForUpdate(@Param("inventoryId") String inventoryId);

    List<Inventory> findByMedicationId(String medicationId);
    List<Inventory> findByMedicationIdAndExpiryDateGreaterThanEqualOrderByExpiryDateAsc(String medicationId,
                                                                                         LocalDate expiryDate);
    List<Inventory> findByStockLevelLessThanEqual(int reorderLevel);
}