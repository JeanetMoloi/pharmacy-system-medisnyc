

import { useEffect, useState } from "react";
import AppShell from "../components/AppShell";
import AdminNav from "../components/AdminNav";
import { Field, SelectField, Alert } from "../components/Field";
import { useAuth } from "../context/useAuth";
import * as orderApi from "../api/orderApi";
import * as inventoryApi from "../api/inventoryApi";
import { parseApiError } from "../utils/parseApiError";
import "./Admin.css";
import "./Appointments.css";
import "./Inventory.css";
import "./Consultations.css";

const STATUSES = ["PENDING", "PROCESSING", "DISPENSED", "COMPLETED", "CANCELLED"];

function emptyItem() {
  return { inventoryId: "" };
}

export default function OrdersList() {
  const { profileId } = useAuth();
  const [orders, setOrders] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyOrderId, setBusyOrderId] = useState(null);

  const [showForm, setShowForm] = useState(false);
  const [patientId, setPatientId] = useState("");
  const [items, setItems] = useState([emptyItem()]);
  const [formError, setFormError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function loadOrders() {
    orderApi
      .getAllOrders()
      .then(setOrders)
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadOrders();
    inventoryApi.getAllInventory().then(setInventory).catch(() => setInventory([]));
  }, []);

  function updateItem(index, inventoryId) {
    setItems((prev) => prev.map((it, i) => (i === index ? { inventoryId } : it)));
  }

  function addItem() {
    setItems((prev) => [...prev, emptyItem()]);
  }

  function removeItem(index) {
    setItems((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleCreate(e) {
    e.preventDefault();
    setFormError("");

    const chosen = items.filter((it) => it.inventoryId);
    if (!patientId.trim()) {
      setFormError("Patient ID is required.");
      return;
    }
    if (chosen.length === 0) {
      setFormError("Add at least one medication batch.");
      return;
    }

    const payloadItems = chosen.map((it) => {
      const batch = inventory.find((inv) => inv.inventoryId === it.inventoryId);
      return { medicationId: batch?.medicationId, inventoryId: it.inventoryId, quantity: 1 };
    });

    setSubmitting(true);
    try {
      await orderApi.createOrder({
        patientId: patientId.trim(),
        pharmacistId: profileId(),
        items: payloadItems,
      });
      setPatientId("");
      setItems([emptyItem()]);
      setShowForm(false);
      loadOrders();
    } catch (err) {
      setFormError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAdvance(order) {
    const idx = STATUSES.indexOf(order.dispensingStatus);
    const next = STATUSES[idx + 1];
    if (!next || next === "CANCELLED") return;
    setBusyOrderId(order.orderId);
    try {
      await orderApi.updateOrderStatus(order.orderId, next);
      loadOrders();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setBusyOrderId(null);
    }
  }

  return (
    <AppShell>
      <div className="appts-header">
        <div>
          <div className="appts-eyebrow">Module 4/6</div>
          <h1>Orders</h1>
          <p className="appts-sub">Dispense medication batches to patients and track fulfilment.</p>
        </div>
        <button className="btn-primary inv-add-btn" onClick={() => setShowForm((v) => !v)}>
          {showForm ? "Cancel" : "+ New order"}
        </button>
      </div>

      <AdminNav />

      <Alert type="error">{error}</Alert>

      {showForm && (
        <div className="book-card" style={{ marginBottom: "24px" }}>
          <h2 style={{ marginTop: 0 }}>New order</h2>
          <form onSubmit={handleCreate} noValidate>
            <Alert type="error">{formError}</Alert>
            <Field
              id="patientId"
              label="Patient ID"
              value={patientId}
              onChange={(e) => setPatientId(e.target.value)}
              placeholder="e.g. PAT1234567890"
              required
            />

            {items.map((item, i) => (
              <div className="rx-item-row" key={i} style={{ gridTemplateColumns: "1fr auto" }}>
                <SelectField
                  label="Medication batch"
                  value={item.inventoryId}
                  onChange={(e) => updateItem(i, e.target.value)}
                >
                  <option value="">Select…</option>
                  {inventory.map((inv) => (
                    <option key={inv.inventoryId} value={inv.inventoryId}>
                      {inv.medicationId} — {inv.stockLevel} in stock — R{inv.unitPrice}
                    </option>
                  ))}
                </SelectField>
                {items.length > 1 && (
                  <button type="button" className="rx-item-remove" onClick={() => removeItem(i)} aria-label="Remove">
                    ×
                  </button>
                )}
              </div>
            ))}

            <button type="button" className="rx-add-item-btn" onClick={addItem}>
              + Add another batch
            </button>

            <button className="btn-primary" type="submit" disabled={submitting} style={{ marginTop: "16px" }}>
              {submitting ? "Placing order…" : "Place order"}
            </button>
          </form>
        </div>
      )}

      {loading ? (
        <p className="appts-loading">Loading orders…</p>
      ) : orders.length === 0 ? (
        <div className="appts-empty">
          <p>No orders yet. Place one above once a prescription is ready.</p>
        </div>
      ) : (
        <table className="admin-table">
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Patient</th>
              <th>Items</th>
              <th>Total</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.orderId}>
                <td style={{ fontFamily: "var(--font-mono)", fontSize: "12px" }}>{o.orderId}</td>
                <td>{o.patientId}</td>
                <td>{(o.items || []).length}</td>
                <td>R{o.totalAmount}</td>
                <td>{o.dispensingStatus}</td>
                <td>
                  {o.dispensingStatus !== "COMPLETED" && o.dispensingStatus !== "CANCELLED" && (
                    <button
                      className="admin-toggle-btn"
                      disabled={busyOrderId === o.orderId}
                      onClick={() => handleAdvance(o)}
                    >
                      Advance
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </AppShell>
  );
}