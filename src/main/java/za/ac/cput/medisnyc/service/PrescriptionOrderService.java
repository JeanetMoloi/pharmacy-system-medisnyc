package za.ac.cput.medisnyc.service;

import za.ac.cput.medisnyc.domain.Order;
import za.ac.cput.medisnyc.domain.OrderStatus;
import za.ac.cput.medisnyc.domain.Prescription;
import za.ac.cput.medisnyc.domain.PrescriptionItem;

import java.util.List;

public interface PrescriptionOrderService {
    Order getByPrescriptionId(String prescriptionId);
    Order createForPrescription(Prescription prescription, List<PrescriptionItem> prescriptionItems);
    void cancelForPrescription(String prescriptionId, String actingUsername);
    Order updateStatus(String orderId, OrderStatus newStatus, String actingUsername);
}
