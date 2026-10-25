import client from "./client";


// Maps onto InventoryController (/api/inventory).

export function getAllInventory() {
    return client.get("/api/inventory").then((res) => res.data);
}

export function getInventorySummary() {
    return client.get("/api/inventory/summary").then((res) => res.data);
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

export function adjustStock(inventoryId, delta, reason) {
    return client
        .put(`/api/inventory/${inventoryId}/adjust`, { delta: Number(delta), reason })
        .then((res) => res.data);
}

export function writeOffExpiredStock(inventoryId, stockLevel) {
    return adjustStock(inventoryId, -Math.abs(Number(stockLevel)), "EXPIRED_WRITE_OFF");
}

export function getLowStockAlerts() {
    return client.get("/api/inventory/low-stock").then((res) => res.data);
}

export function getExpiringSoon(days = 30) {
    return client.get("/api/inventory/expiring-soon", { params: { days } }).then((res) => res.data);
}

export function getExpiredStock() {
    return client.get("/api/inventory/expired").then((res) => res.data);
}