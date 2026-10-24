package za.ac.cput.medisnyc.repository.jpa;

import za.ac.cput.medisnyc.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogJpaRepository extends JpaRepository<AuditLog, Long> {
    @Query("select a from AuditLog a where (:username is null or lower(a.username) like lower(concat('%', :username, '%'))) "
            + "and (:action is null or lower(a.action) like lower(concat('%', :action, '%')))")
    Page<AuditLog> search(@Param("username") String username,
                          @Param("action") String action,
                          Pageable pageable);
}
