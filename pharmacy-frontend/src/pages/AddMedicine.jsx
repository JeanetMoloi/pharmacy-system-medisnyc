import { useState } from "react";
import { useNavigate } from "react-router-dom";
import AppShell from "../components/AppShell";
import { Field, Alert } from "../components/Field";
import * as medicationApi from "../api/medicationApi";
import * as inventoryApi from "../api/inventoryApi";
import { parseApiError } from "../utils/parseApiError";
import "../components/Form.css";
import "./Inventory.css";

const EMPTY_MED = { medicationName: "", dosageForm: "", manufacturer: "", strength: "", category: "", description: "" };
const EMPTY_STOCK = { stockLevel: "", batchNumber: "", supplier: "", unitPrice: "", expiryDate: "", receivedDate: "", reorderLevel: "10" };

export default function AddMedicine() {
    const navigate = useNavigate();
    const [med, setMed] = useState(EMPTY_MED);
    const [addStock, setAddStock] = useState(true);
    const [stock, setStock] = useState(EMPTY_STOCK);
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    function updateMed(field, value) {
        setMed((prev) => ({ ...prev, [field]: value }));
    }
    function updateStock(field, value) {
        setStock((prev) => ({ ...prev, [field]: value }));
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSubmitting(true);
        try {
            const saved = await medicationApi.addMedication(med);
            if (addStock && stock.stockLevel) {
                await inventoryApi.receiveStock({ ...stock, medicationId: saved.medicationId });
            }
            navigate("/inventory", { replace: true });
        } catch (err) {
            setError(parseApiError(err).message);
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <AppShell>
            <div className="appts-eyebrow">Module 4</div>
            <h1>Add medicine</h1>
            <p className="appts-sub" style={{ marginBottom: "24px" }}>
                Add it to the catalogue, and optionally receive an initial batch of stock.
            </p>

            <div className="consult-card">
                <form onSubmit={handleSubmit} noValidate>
                    <Alert type="error">{error}</Alert>

                    <Field
                        id="medicationName"
                        label="Medicine name"
                        value={med.medicationName}
                        onChange={(e) => updateMed("medicationName", e.target.value)}
                        required
                    />
                    <div className="field-row">
                        <Field
                            id="strength"
                            label="Strength"
                            value={med.strength}
                            onChange={(e) => updateMed("strength", e.target.value)}
                            placeholder="e.g. 500mg"
                        />
                        <Field
                            id="dosageForm"
                            label="Dosage form"
                            value={med.dosageForm}
                            onChange={(e) => updateMed("dosageForm", e.target.value)}
                            placeholder="e.g. Tablet"
                        />
                    </div>
                    <div className="field-row">
                        <Field
                            id="manufacturer"
                            label="Manufacturer"
                            value={med.manufacturer}
                            onChange={(e) => updateMed("manufacturer", e.target.value)}
                        />
                        <Field
                            id="category"
                            label="Category"
                            value={med.category}
                            onChange={(e) => updateMed("category", e.target.value)}
                            placeholder="e.g. Analgesic"
                        />
                    </div>
                    <Field
                        id="description"
                        label="Description (optional)"
                        value={med.description}
                        onChange={(e) => updateMed("description", e.target.value)}
                    />

                    <label style={{ display: "flex", alignItems: "center", gap: "8px", margin: "20px 0", fontSize: "14px", fontWeight: 600 }}>
                        <input type="checkbox" checked={addStock} onChange={(e) => setAddStock(e.target.checked)} />
                        Receive an initial batch of stock now
                    </label>

                    {addStock && (
                        <>
                            <div className="field-row">
                                <Field
                                    id="stockLevel"
                                    label="Quantity"
                                    type="number"
                                    min="0"
                                    value={stock.stockLevel}
                                    onChange={(e) => updateStock("stockLevel", e.target.value)}
                                />
                                <Field
                                    id="reorderLevel"
                                    label="Reorder level"
                                    type="number"
                                    min="0"
                                    value={stock.reorderLevel}
                                    onChange={(e) => updateStock("reorderLevel", e.target.value)}
                                />
                            </div>
                            <div className="field-row">
                                <Field
                                    id="batchNumber"
                                    label="Batch number"
                                    value={stock.batchNumber}
                                    onChange={(e) => updateStock("batchNumber", e.target.value)}
                                />
                                <Field
                                    id="supplier"
                                    label="Supplier"
                                    value={stock.supplier}
                                    onChange={(e) => updateStock("supplier", e.target.value)}
                                />
                            </div>
                            <div className="field-row">
                                <Field
                                    id="unitPrice"
                                    label="Unit price (R)"
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={stock.unitPrice}
                                    onChange={(e) => updateStock("unitPrice", e.target.value)}
                                />
                                <Field
                                    id="expiryDate"
                                    label="Expiry date"
                                    type="date"
                                    value={stock.expiryDate}
                                    onChange={(e) => updateStock("expiryDate", e.target.value)}
                                />
                            </div>
                        </>
                    )}

                    <button className="btn-primary" type="submit" disabled={submitting}>
                        {submitting ? "Saving…" : "Add medicine"}
                    </button>
                </form>
            </div>
        </AppShell>
    );
}
