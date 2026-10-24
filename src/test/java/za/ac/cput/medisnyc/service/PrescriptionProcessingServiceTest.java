package za.ac.cput.medisnyc.service;

import org.junit.jupiter.api.Test;
import za.ac.cput.medisnyc.domain.Order;
import za.ac.cput.medisnyc.domain.OrderStatus;
import za.ac.cput.medisnyc.domain.Prescription;
import za.ac.cput.medisnyc.domain.PrescriptionProcessingStatus;
import za.ac.cput.medisnyc.domain.PrescriptionStatus;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionCollectionJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionItemJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.AuditLogJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.NotificationJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

class PrescriptionProcessingServiceTest {

    @Test
    void advancesToPreparingAfterCreatingPrescriptionOrder() {
        Prescription existing = prescription(PrescriptionProcessingStatus.RECEIVED);
        PrescriptionJpaRepository prescriptions = mock(PrescriptionJpaRepository.class);
        when(prescriptions.findByIdForUpdate("RX-1")).thenReturn(Optional.of(existing));
        when(prescriptions.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PrescriptionOrderService orders = mock(PrescriptionOrderService.class);
        when(orders.getByPrescriptionId("RX-1")).thenReturn(new Order.Builder()
                .setOrderId("ORD-1")
                .setPatientId("PAT-1")
                .setPrescriptionId("RX-1")
                .setDispensingStatus(OrderStatus.PROCESSING)
                .build());
        PrescriptionProcessingService service = service(prescriptions, orders);

        Prescription updated = service.advanceStatus("RX-1");

        assertEquals(PrescriptionProcessingStatus.PREPARING, updated.getProcessingStatus());
        assertEquals("ORD-1", updated.getLinkedOrderId());
        verify(orders).createForPrescription(any(Prescription.class), any());
    }

    @Test
    void doesNotAdvanceOrNotifyWhenStockIsInsufficient() {
        Prescription existing = prescription(PrescriptionProcessingStatus.RECEIVED);
        PrescriptionJpaRepository prescriptions = mock(PrescriptionJpaRepository.class);
        when(prescriptions.findByIdForUpdate("RX-1")).thenReturn(Optional.of(existing));
        PrescriptionOrderService orders = mock(PrescriptionOrderService.class);
        when(orders.createForPrescription(any(Prescription.class), any()))
                .thenThrow(new IllegalStateException("Insufficient stock for medications: MED-1"));
        NotificationJpaRepository notificationRepository = mock(NotificationJpaRepository.class);
        NotificationService notifications = new NotificationService(notificationRepository);
        PrescriptionProcessingService service = new PrescriptionProcessingService(
                prescriptions, mock(PrescriptionCollectionJpaRepository.class), notifications,
                new AuditLogService(mock(AuditLogJpaRepository.class)),
                mock(PrescriptionItemJpaRepository.class), orders);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.advanceStatus("RX-1"));

        assertEquals("Insufficient stock for medications: MED-1", error.getMessage());
        verify(prescriptions, never()).save(any(Prescription.class));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void completesLinkedOrderWhenPrescriptionIsCollected() {
        Prescription existing = prescription(PrescriptionProcessingStatus.READY_FOR_COLLECTION);
        PrescriptionJpaRepository prescriptions = mock(PrescriptionJpaRepository.class);
        when(prescriptions.findByIdForUpdate("RX-1")).thenReturn(Optional.of(existing));
        when(prescriptions.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PrescriptionOrderService orders = mock(PrescriptionOrderService.class);
        when(orders.getByPrescriptionId("RX-1")).thenReturn(new Order.Builder()
                .setOrderId("ORD-1")
                .setPatientId("PAT-1")
                .setPrescriptionId("RX-1")
                .setDispensingStatus(OrderStatus.PROCESSING)
                .build());
        PrescriptionProcessingService service = service(prescriptions, orders);

        Prescription collected = service.advanceStatus("RX-1");

        assertEquals(PrescriptionProcessingStatus.COLLECTED, collected.getProcessingStatus());
        var orderSequence = inOrder(orders);
        orderSequence.verify(orders).updateStatus("ORD-1", OrderStatus.DISPENSED, "SYSTEM");
        orderSequence.verify(orders).updateStatus("ORD-1", OrderStatus.COMPLETED, "SYSTEM");
    }

    @Test
    void cancelsLinkedOrderWhenPrescriptionIsCancelled() {
        Prescription existing = prescription(PrescriptionProcessingStatus.PREPARING);
        PrescriptionJpaRepository prescriptions = mock(PrescriptionJpaRepository.class);
        when(prescriptions.findById("RX-1")).thenReturn(Optional.of(existing));
        when(prescriptions.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PrescriptionOrderService orders = mock(PrescriptionOrderService.class);
        PrescriptionProcessingService service = service(prescriptions, orders);

        Prescription cancelled = service.updateClinicalStatus("RX-1", PrescriptionStatus.CANCELLED);

        assertEquals(PrescriptionStatus.CANCELLED, cancelled.getStatus());
        verify(orders).cancelForPrescription("RX-1", "SYSTEM");
    }

    private PrescriptionProcessingService service(PrescriptionJpaRepository prescriptions,
                                                   PrescriptionOrderService orders) {
        return new PrescriptionProcessingService(prescriptions, mock(PrescriptionCollectionJpaRepository.class),
                new NotificationService(mock(NotificationJpaRepository.class)),
                new AuditLogService(mock(AuditLogJpaRepository.class)),
                mock(PrescriptionItemJpaRepository.class), orders);
    }

    private Prescription prescription(PrescriptionProcessingStatus processingStatus) {
        return new Prescription.Builder()
                .setPrescriptionId("RX-1")
                .setPatientId("PAT-1")
                .setDoctorId("DOC-1")
                .setDateIssued(LocalDate.now())
                .setExpiryDate(LocalDate.now().plusDays(30))
                .setStatus(PrescriptionStatus.ACTIVE)
                .setProcessingStatus(processingStatus)
                .build();
    }
}
