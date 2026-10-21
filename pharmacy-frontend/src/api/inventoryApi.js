inventoryApi.js
import client from "./client";


// Maps onto InventoryController (/api/inventory).

export function getAllInventory() {
    return client.get("/api/inventory").then((res) => res.data);
}

export function getInventoryByMedication(medicationId) {
    return client.get(`/api/inventory/medication/${medicationId}`).then((res) => res.data);
}

export function receiveStock({ medicationId, stockLevel, expiryDate, batchNumber, supplier, unitPrice, receivedDate, reorderLevel }) {
    return client
        .post("/api/inventory", {
            medicationId,
            stockLevel: Number(stockLevel) || 0,
            expiryDate,
            batchNumber,
            supplier,
            unitPrice: Number(unitPrice) || 0,
            receivedDate,
            reorderLevel: Number(reorderLevel) || 10,
        })
        .then((res) => res.data);
}

export function updateStock(inventoryId, stockLevel) {
    return client
        .put(`/api/inventory/${inventoryId}/stock`, null, { params: { stockLevel } })
        .then((res) => res.data);
}

export function getLowStockAlerts() {
    return client.get("/api/inventory/low-stock").then((res) => res.data);
}

export function getExpiringSoon(days = 30) {
    return client.get("/api/inventory/expiring-soon", { params: { days } }).then((res) => res.data);
}