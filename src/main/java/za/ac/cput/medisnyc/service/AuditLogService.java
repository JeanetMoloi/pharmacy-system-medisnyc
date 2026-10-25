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

    private static final int USERNAME_MAX_LENGTH = 255;
    private static final int ACTION_MAX_LENGTH = 100;
    private static final int ENTITY_AFFECTED_MAX_LENGTH = 255;
    private static final int DETAILS_MAX_LENGTH = 2000;

    private final AuditLogJpaRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogJpaRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String username, String action, String entityAffected, String details) {
        AuditLog entry = new AuditLog.Builder()
                .setUsername(limitLength(username, USERNAME_MAX_LENGTH))
                .setAction(limitLength(action, ACTION_MAX_LENGTH))
                .setEntityAffected(limitLength(entityAffected, ENTITY_AFFECTED_MAX_LENGTH))
                .setDetails(limitLength(details, DETAILS_MAX_LENGTH))
                .build();
        auditLogRepository.save(entry);
    }

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
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
