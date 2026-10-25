package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import za.ac.cput.medisnyc.domain.AuditLog;
import za.ac.cput.medisnyc.repository.jpa.AuditLogJpaRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogServiceTest {

    private final AuditLogJpaRepository repository = mock(AuditLogJpaRepository.class);
    private final AuditLogService service = new AuditLogService(repository);

    @Test
    void truncatesOversizedDetailsToColumnLimitWithoutThrowing() {
        String details = "x".repeat(5000);

        service.log("user", "ACTION", "entity", details);

        ArgumentCaptor<AuditLog> entry = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(entry.capture());
        assertEquals(2000, entry.getValue().getDetails().length());
        assertTrue(entry.getValue().getDetails().endsWith("..."));
    }

    @Test
    void acceptsNullAuditText() {
        service.log(null, null, null, null);

        ArgumentCaptor<AuditLog> entry = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(entry.capture());
        assertNull(entry.getValue().getUsername());
        assertNull(entry.getValue().getAction());
        assertNull(entry.getValue().getEntityAffected());
        assertNull(entry.getValue().getDetails());
    }
}
