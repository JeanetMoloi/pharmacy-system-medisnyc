
import { useEffect, useState } from "react";
import AppShell from "../components/AppShell";
import AdminNav from "../components/AdminNav";
import { Alert } from "../components/Field";
import * as adminApi from "../api/adminApi.js";
import { parseApiError } from "../utils/parseApiError";
import "./Admin.css";

export default function AuditLog() {
    const [logs, setLogs] = useState([]);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        adminApi
            .getAuditLogs()
            .then((data) => setLogs([...data].sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))))
            .catch((err) => setError(parseApiError(err).message))
            .finally(() => setLoading(false));
    }, []);

    return (
        <AppShell>
            <div className="appts-eyebrow">Module 6</div>
            <h1>Audit log</h1>
            <p className="appts-sub" style={{ marginBottom: "20px" }}>
                A record of actions taken across the system.
            </p>

            <AdminNav />

            <Alert type="error">{error}</Alert>

            {loading ? (
                <p className="appts-loading">Loading audit log…</p>
            ) : logs.length === 0 ? (
                <p className="inv-empty">No audit entries yet.</p>
            ) : (
                <table className="admin-table">
                    <thead>
                    <tr>
                        <th>When</th>
                        <th>User</th>
                        <th>Action</th>
                        <th>Entity</th>
                        <th>Details</th>
                    </tr>
                    </thead>
                    <tbody>
                    {logs.map((log) => (
                        <tr key={log.logId}>
                            <td>{new Date(log.timestamp).toLocaleString()}</td>
                            <td>{log.username}</td>
                            <td>{log.action}</td>
                            <td>{log.entityAffected}</td>
                            <td>{log.details}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            )}
        </AppShell>
    );
}