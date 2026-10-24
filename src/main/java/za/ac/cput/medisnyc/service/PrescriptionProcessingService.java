package za.ac.cput.medisnyc.service;

/* PrescriptionProcessingService.java
   Module 5: Prescription Processing Module.
   Enforces the Pending -> Received -> Preparing -> Ready for Collection -> Collected flow.
   Author: Naledi Ngobeni
*/

import za.ac.cput.medisnyc.domain.Prescription;
import za.ac.cput.medisnyc.domain.PrescriptionCollection;
import za.ac.cput.medisnyc.domain.PrescriptionProcessingStatus;
import za.ac.cput.medisnyc.domain.PrescriptionStatus;
import za.ac.cput.medisnyc.domain.Order;
import za.ac.cput.medisnyc.domain.OrderStatus;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionCollectionJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionItemJpaRepository;
import za.ac.cput.medisnyc.repository.jpa.PrescriptionJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class PrescriptionProcessingService {

    private final PrescriptionJpaRepository prescriptionRepository;
    private final PrescriptionCollectionJpaRepository collectionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final PrescriptionItemJpaRepository prescriptionItemRepository;
    private final PrescriptionOrderService orderService;

    // Defines which status can move to which next status.
    private static final Map<PrescriptionProcessingStatus, PrescriptionProcessingStatus> NEXT_STATUS = Map.of(
            PrescriptionProcessingStatus.PENDING, PrescriptionProcessingStatus.RECEIVED,
            PrescriptionProcessingStatus.RECEIVED, PrescriptionProcessingStatus.PREPARING,
            PrescriptionProcessingStatus.PREPARING, PrescriptionProcessingStatus.READY_FOR_COLLECTION,
            PrescriptionProcessingStatus.READY_FOR_COLLECTION, PrescriptionProcessingStatus.COLLECTED
    );

    @Autowired
    public PrescriptionProcessingService(PrescriptionJpaRepository prescriptionRepository,
                                         PrescriptionCollectionJpaRepository collectionRepository,
                                         NotificationService notificationService,
                                         AuditLogService auditLogService,
                                         PrescriptionItemJpaRepository prescriptionItemRepository,
                                         PrescriptionOrderService orderService) {
        this.prescriptionRepository = prescriptionRepository;
        this.collectionRepository = collectionRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.orderService = orderService;
    }

    public List<Prescription> getQueue() {
        // Everything not yet collected - the pharmacist's prescription queue.
        return prescriptionRepository.findAll().stream()
                .filter(p -> p.getStatus() == PrescriptionStatus.ACTIVE
                        && p.getProcessingStatus() != PrescriptionProcessingStatus.COLLECTED)
                .map(p -> withLinkedOrder(p, orderService.getByPrescriptionId(p.getPrescriptionId())))
                .toList();
    }

    // Module 5: Prescription Tracking - lets a patient see their own prescriptions
    // and follow each one through Pending -> ... -> Collected.
    public List<Prescription> getByPatient(String patientId) {
        return prescriptionRepository.findByPatientId(patientId);
    }

    public List<Prescription> getByDoctor(String doctorId) {
        return prescriptionRepository.findByDoctorId(doctorId);
    }

    public List<Prescription> getReadyForCollection() {
        return prescriptionRepository.findByProcessingStatus(PrescriptionProcessingStatus.READY_FOR_COLLECTION);
    }

    public Prescription getById(String prescriptionId) {
        return prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + prescriptionId));
    }

    @Transactional
    public Prescription advanceStatus(String prescriptionId) {
        Prescription existing = prescriptionRepository.findByIdForUpdate(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + prescriptionId));
        if (existing.getStatus() != PrescriptionStatus.ACTIVE) {
            throw new IllegalStateException("Prescription is " + existing.getStatus().name().toLowerCase());
        }
        if (existing.getExpiryDate() != null && existing.getExpiryDate().isBefore(LocalDate.now())) {
            Prescription expired = copyWithStatus(existing, PrescriptionStatus.EXPIRED);
            Prescription savedExpired = prescriptionRepository.save(expired);
            orderService.cancelForPrescription(prescriptionId, "SYSTEM");
            auditLogService.log("SYSTEM", "EXPIRE_PRESCRIPTION", prescriptionId,
                    "Prescription expired before collection");
            return withLinkedOrder(savedExpired, orderService.getByPrescriptionId(prescriptionId));
        }
        PrescriptionProcessingStatus current = existing.getProcessingStatus();
        PrescriptionProcessingStatus next = NEXT_STATUS.get(current);

        if (next == null) {
            throw new IllegalStateException("Prescription is already at its final status: " + current);
        }

        if (next == PrescriptionProcessingStatus.PREPARING) {
            orderService.createForPrescription(existing,
                    prescriptionItemRepository.findByPrescriptionId(prescriptionId));
        }

        Prescription updated = new Prescription.Builder()
                .setPrescriptionId(existing.getPrescriptionId())
                .setPatientId(existing.getPatientId())
                .setDoctorId(existing.getDoctorId())
                .setDateIssued(existing.getDateIssued())
                .setExpiryDate(existing.getExpiryDate())
                .setInstructions(existing.getInstructions())
                .setRefillsAllowed(existing.getRefillsAllowed())
                .setRefillsUsed(existing.getRefillsUsed())
                .setStatus(existing.getStatus())
                .setProcessingStatus(next)
                .build();

        Prescription saved = prescriptionRepository.save(updated);
        auditLogService.log("SYSTEM", "ADVANCE_PRESCRIPTION", saved.getPrescriptionId(),
                "Processing status changed from " + current + " to " + next);

        if (next == PrescriptionProcessingStatus.READY_FOR_COLLECTION) {
            notificationService.notify(saved.getPatientId(), saved.getPrescriptionId(),
                    "Your prescription " + saved.getPrescriptionId() + " is ready for collection.");
        }

        if (next == PrescriptionProcessingStatus.COLLECTED) {
            PrescriptionCollection collection = new PrescriptionCollection.Builder()
                    .setCollectionId("COL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .setPrescriptionId(saved.getPrescriptionId())
                    .setPatientId(saved.getPatientId())
                    .build();
            collectionRepository.save(collection);
            var order = orderService.getByPrescriptionId(saved.getPrescriptionId());
            if (order == null) {
                throw new IllegalStateException("No order is linked to prescription " + saved.getPrescriptionId());
            }
            orderService.updateStatus(order.getOrderId(), OrderStatus.DISPENSED, "SYSTEM");
            orderService.updateStatus(order.getOrderId(), OrderStatus.COMPLETED, "SYSTEM");
        }

        return withLinkedOrder(saved, orderService.getByPrescriptionId(saved.getPrescriptionId()));
    }

    @Transactional
    public Prescription updateClinicalStatus(String prescriptionId, PrescriptionStatus status) {
        if (status == null || status == PrescriptionStatus.ACTIVE) {
            throw new IllegalArgumentException("Prescription status must be CANCELLED or EXPIRED");
        }
        Prescription existing = getById(prescriptionId);
        if (existing.getProcessingStatus() == PrescriptionProcessingStatus.COLLECTED) {
            throw new IllegalStateException("Collected prescriptions cannot be cancelled or expired");
        }
        Prescription updated = copyWithStatus(existing, status);
        Prescription saved = prescriptionRepository.save(updated);
        orderService.cancelForPrescription(prescriptionId, "SYSTEM");
        auditLogService.log("SYSTEM", "UPDATE_PRESCRIPTION_STATUS", prescriptionId,
                "Prescription status changed to " + status);
        return withLinkedOrder(saved, orderService.getByPrescriptionId(prescriptionId));
    }

    private Prescription copyWithStatus(Prescription prescription, PrescriptionStatus status) {
        return new Prescription.Builder()
                .setPrescriptionId(prescription.getPrescriptionId())
                .setPatientId(prescription.getPatientId())
                .setDoctorId(prescription.getDoctorId())
                .setDateIssued(prescription.getDateIssued())
                .setExpiryDate(prescription.getExpiryDate())
                .setInstructions(prescription.getInstructions())
                .setRefillsAllowed(prescription.getRefillsAllowed())
                .setRefillsUsed(prescription.getRefillsUsed())
                .setStatus(status)
                .setProcessingStatus(prescription.getProcessingStatus())
                .build();
    }

    private Prescription withLinkedOrder(Prescription prescription, Order order) {
        return new Prescription.Builder()
                .setPrescriptionId(prescription.getPrescriptionId())
                .setPatientId(prescription.getPatientId())
                .setDoctorId(prescription.getDoctorId())
                .setDateIssued(prescription.getDateIssued())
                .setExpiryDate(prescription.getExpiryDate())
                .setInstructions(prescription.getInstructions())
                .setRefillsAllowed(prescription.getRefillsAllowed())
                .setRefillsUsed(prescription.getRefillsUsed())
                .setStatus(prescription.getStatus())
                .setProcessingStatus(prescription.getProcessingStatus())
                .setLinkedOrderId(order == null ? null : order.getOrderId())
                .build();
    }
}