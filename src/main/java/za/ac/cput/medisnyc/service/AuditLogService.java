package za.ac.cput.medisnyc.service;

/* AuditLogService.java
   Module 6: Reports & Administration - Audit Log.
*/

import za.ac.cput.medisnyc.domain.AuditLog;
import za.ac.cput.medisnyc.repository.jpa.AuditLogJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogJpaRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogJpaRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String username, String action, String entityAffected, String details) {
        AuditLog entry = new AuditLog.Builder()
                .setUsername(username)
                .setAction(action)
                .setEntityAffected(entityAffected)
                .setDetails(details)
                .build();
        auditLogRepository.save(entry);
    }

    public List<AuditLog> getAll() {
        return auditLogRepository.findAll();
    }

    public Page<AuditLog> getPage(int page, int size, String username, String action) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        return auditLogRepository.search(blankToNull(username), blankToNull(action),
                PageRequest.of(safePage, safeSize, Sort.by(
                        Sort.Order.desc("timestamp"), Sort.Order.desc("logId"))));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
