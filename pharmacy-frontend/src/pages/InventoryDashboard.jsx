

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import AppShell from "../components/AppShell";
import { Alert } from "../components/Field";
import * as medicationApi from "../api/medicationApi";
import * as inventoryApi from "../api/inventoryApi";
import { parseApiError } from "../utils/parseApiError";
import "./Inventory.css";

function StockEditor({ item, onSave }) {
  const [value, setValue] = useState(item.stockLevel);
  const [saving, setSaving] = useState(false);
  const changed = Number(value) !== item.stockLevel;

  async function save() {
    setSaving(true);
    try {
      await onSave(item.inventoryId, Number(value));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="inv-stock-cell">
      <input
        className="inv-stock-input"
        type="number"
        min="0"
        value={value}
        onChange={(e) => setValue(e.target.value)}
      />
      {changed && (
        <button className="inv-stock-save" onClick={save} disabled={saving}>
          {saving ? "…" : "Save"}
        </button>
      )}
    </div>
  );
}

export default function InventoryDashboard() {
  const [medications, setMedications] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  function loadAll() {
    setLoading(true);
    Promise.all([medicationApi.getAllMedications(), inventoryApi.getAllInventory(), inventoryApi.getLowStockAlerts()])
      .then(([meds, inv, low]) => {
        setMedications(meds);
        setInventory(inv);
        setLowStock(low);
      })
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }

  useEffect(loadAll, []);

  async function handleUpdateStock(inventoryId, stockLevel) {
    try {
      const updated = await inventoryApi.updateStock(inventoryId, stockLevel);
      setInventory((prev) => prev.map((i) => (i.inventoryId === inventoryId ? updated : i)));
      inventoryApi.getLowStockAlerts().then(setLowStock).catch(() => {});
    } catch (err) {
      setError(parseApiError(err).message);
    }
  }

  const lowStockIds = new Set(lowStock.map((i) => i.inventoryId));
  const medicationName = (id) => medications.find((m) => m.medicationId === id)?.medicationName || id;

  return (
    <AppShell>
      <div className="appts-eyebrow">Module 4</div>
      <h1>Pharmacy inventory</h1>
      <p className="appts-sub" style={{ marginBottom: "28px" }}>
        Manage the medicine catalogue and keep stock levels current.
      </p>

      <Alert type="error">{error}</Alert>

      {!loading && lowStock.length > 0 && (
        <div className="inv-alert-banner">
          ⚠ {lowStock.length} item{lowStock.length > 1 ? "s are" : " is"} at or below their reorder level.
        </div>
      )}

      {loading ? (
        <p className="appts-loading">Loading inventory…</p>
      ) : (
        <>
          <div className="inv-section">
            <div className="inv-section-head">
              <h2>Medicine list</h2>
              <Link to="/inventory/add" className="btn-primary inv-add-btn">
                + Add medicine
              </Link>
            </div>
            {medications.length === 0 ? (
              <p className="inv-empty">No medications in the catalogue yet.</p>
            ) : (
              <table className="inv-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Form</th>
                    <th>Strength</th>
                    <th>Manufacturer</th>
                    <th>Category</th>
                  </tr>
                </thead>
                <tbody>
                  {medications.map((m) => (
                    <tr key={m.medicationId}>
                      <td>{m.medicationName}</td>
                      <td>{m.dosageForm || "—"}</td>
                      <td>{m.strength || "—"}</td>
                      <td>{m.manufacturer || "—"}</td>
                      <td>{m.category || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <div className="inv-section">
            <div className="inv-section-head">
              <h2>Stock levels</h2>
            </div>
            {inventory.length === 0 ? (
              <p className="inv-empty">No stock received yet.</p>
            ) : (
              <table className="inv-table">
                <thead>
                  <tr>
                    <th>Medicine</th>
                    <th>Batch</th>
                    <th>Stock</th>
                    <th>Reorder level</th>
                    <th>Expiry</th>
                  </tr>
                </thead>
                <tbody>
                  {inventory.map((i) => (
                    <tr key={i.inventoryId} className={lowStockIds.has(i.inventoryId) ? "low-stock" : ""}>
                      <td>{medicationName(i.medicationId)}</td>
                      <td>{i.batchNumber || "—"}</td>
                      <td>
                        <StockEditor item={i} onSave={handleUpdateStock} />
                      </td>
                      <td>{i.reorderLevel}</td>
                      <td>{i.expiryDate || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </AppShell>
  );
}