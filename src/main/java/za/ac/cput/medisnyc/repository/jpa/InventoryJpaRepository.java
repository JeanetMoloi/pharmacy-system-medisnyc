package za.ac.cput.medisnyc.repository.jpa;

import za.ac.cput.medisnyc.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
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

    boolean existsByMedicationIdAndStockLevelGreaterThan(String medicationId, int stockLevel);
    void deleteByMedicationIdAndStockLevelLessThanEqual(String medicationId, int stockLevel);
    boolean existsByMedicationIdAndBatchNumberIgnoreCase(String medicationId, String batchNumber);
    boolean existsByBatchNumberIgnoreCase(String batchNumber);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Inventory i set i.stockLevel = i.stockLevel + :delta
            where i.inventoryId = :inventoryId
              and i.stockLevel + :delta >= 0
              and (:delta <= 0 or i.expiryDate >= :today)
            """)
    int adjustStockAtomically(@Param("inventoryId") String inventoryId, @Param("delta") int delta,
                              @Param("today") LocalDate today);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Inventory i set i.stockLevel = 0
            where i.inventoryId = :inventoryId and i.stockLevel > 0 and i.expiryDate < :today
            """)
    int writeOffExpiredStock(@Param("inventoryId") String inventoryId, @Param("today") LocalDate today);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Inventory i set i.stockLevel = i.stockLevel - :quantity
            where i.inventoryId = :inventoryId and i.stockLevel >= :quantity
              and i.expiryDate >= :today
            """)
    int decrementStockAtomically(@Param("inventoryId") String inventoryId, @Param("quantity") int quantity,
                                 @Param("today") LocalDate today);

    @Query("""
            select i from Inventory i
            where i.stockLevel > 0 and i.expiryDate between :today and :until
            order by i.expiryDate asc
            """)
    List<Inventory> findExpiringStock(@Param("today") LocalDate today, @Param("until") LocalDate until);

    @Query("""
            select i from Inventory i
            where i.stockLevel > 0 and i.expiryDate < :today
            order by i.expiryDate asc
            """)
    List<Inventory> findExpiredStock(@Param("today") LocalDate today);

    @Query("""
            select m.medicationId, m.medicationName, m.dosageForm, m.strength,
                   coalesce(sum(case when i.expiryDate >= :today then i.stockLevel else 0 end), 0),
                   count(i), min(case when i.expiryDate >= :today then i.expiryDate else null end),
                   coalesce(max(case when i.expiryDate >= :today then i.reorderLevel else 0 end), 0),
                   coalesce(sum(case when i.stockLevel > 0 and i.expiryDate < :today then 1 else 0 end), 0),
                   coalesce(sum(case when i.stockLevel > 0 and i.expiryDate between :today and :until then 1 else 0 end), 0)
            from Medication m left join Inventory i on i.medicationId = m.medicationId
            group by m.medicationId, m.medicationName, m.dosageForm, m.strength
            order by m.medicationName
            """)
    List<Object[]> getInventorySummary(@Param("today") LocalDate today, @Param("until") LocalDate until);

    @Query("""
            select i from Inventory i
            where i.stockLevel > 0 and i.expiryDate >= :today
              and i.medicationId in (
                  select available.medicationId from Inventory available
                  where available.expiryDate >= :today
                  group by available.medicationId
                  having sum(available.stockLevel) <= max(available.reorderLevel)
              )
            order by i.expiryDate asc
            """)
    List<Inventory> findLowStock(@Param("today") LocalDate today);
}