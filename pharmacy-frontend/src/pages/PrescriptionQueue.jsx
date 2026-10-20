import { useEffect, useState } from "react";
import AppShell from "../components/AppShell";
import { Alert } from "../components/Field";
import StatusBadge from "../components/StatusBadge";
import * as processingApi from "../api/prescriptionProcessingApi";
import { parseApiError } from "../utils/parseApiError";
import "./Appointments.css";
import "./Prescriptions.css";

const POLL_INTERVAL_MS = 10000;

const NEXT_LABEL = {
    PENDING: "Mark received",
    RECEIVED: "Start preparing",
    PREPARING: "Mark ready for collection",
    READY_FOR_COLLECTION: "Mark collected",
};

export default function PrescriptionQueue() {
    const [queue, setQueue] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [advancingId, setAdvancingId] = useState(null);

    function load() {
        setLoading(true);
        processingApi
            .getQueue()
            .then(setQueue)
            .catch((err) => setError(parseApiError(err).message))
            .finally(() => setLoading(false));
    }

    useEffect(() => {
        load();
        const timer = setInterval(() => {
            // Don't refetch out from under an in-flight advance click.
            setAdvancingId((current) => {
                if (!current) load();
                return current;
            });
        }, POLL_INTERVAL_MS);
        return () => clearInterval(timer);
    }, []);

    async function handleAdvance(prescriptionId) {
        setAdvancingId(prescriptionId);
        setError("");
        try {
            const updated = await processingApi.advanceStatus(prescriptionId);
            setQueue((prev) =>
                updated.processingStatus === "COLLECTED"
                    ? prev.filter((p) => p.prescriptionId !== prescriptionId)
                    : prev.map((p) => (p.prescriptionId === prescriptionId ? updated : p))
            );
        } catch (err) {
            setError(parseApiError(err).message);
        } finally {
            setAdvancingId(null);
        }
    }

    return (
        <AppShell>
            <div className="appts-eyebrow">Module 5</div>
            <h1>Prescription queue</h1>
            <p className="appts-sub" style={{ marginBottom: "24px" }}>
                Pending → Received → Preparing → Ready for Collection → Collected.
            </p>

            <Alert type="error">{error}</Alert>

            {loading ? (
                <p className="appts-loading">Loading queue…</p>
            ) : queue.length === 0 ? (
                <div className="appts-empty">
                    <p>Nothing in the queue — everything's been collected.</p>
                </div>
            ) : (
                <div className="appts-list">
                    {queue.map((p) => (
                        <div className="appts-row" key={p.prescriptionId}>
                            <div className="appts-row-main">
                                <div className="appts-row-id">{p.prescriptionId}</div>
                                <div className="appts-row-parties">Patient: {p.patientId}</div>
                                <div className="appts-row-parties">Doctor: {p.doctorId}</div>
                            </div>
                            <div className="appts-row-side">
                                <StatusBadge status={p.processingStatus} />
                                <button
                                    className="rx-queue-advance-btn"
                                    onClick={() => handleAdvance(p.prescriptionId)}
                                    disabled={advancingId === p.prescriptionId}
                                >
                                    {advancingId === p.prescriptionId ? "Updating…" : NEXT_LABEL[p.processingStatus] || "Advance"}
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </AppShell>
    );
}