
import { useEffect, useState } from "react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import AppShell from "../components/AppShell";
import AdminNav from "../components/AdminNav";
import { Alert } from "../components/Field";
import * as adminApi from "../api/adminApi.js";
import { parseApiError } from "../utils/parseApiError";
import "./Admin.css";

const CHART_LABELS = {
  totalPatients: "Patients",
  totalAppointments: "Appointments",
  totalPrescriptions: "Prescriptions",
  totalOrders: "Orders",
  lowStockItems: "Low stock",
  prescriptionsAwaitingCollection: "Awaiting collection",
};

export default function Reports() {
  const [report, setReport] = useState(null);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([adminApi.getRevenueReport(), adminApi.getDashboardStatistics()])
      .then(([revenue, dashboardStats]) => {
        setReport(revenue);
        setStats(dashboardStats);
      })
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }, []);

  const chartData = stats
    ? Object.entries(CHART_LABELS).map(([key, label]) => ({ name: label, value: stats[key] || 0 }))
    : [];

  return (
    <AppShell>
      <div className="appts-eyebrow">Module 6</div>
      <h1>Reports</h1>
      <p className="appts-sub" style={{ marginBottom: "20px" }}>
        Revenue and system-wide activity.
      </p>

      <AdminNav />

      <Alert type="error">{error}</Alert>

      {loading ? (
        <p className="appts-loading">Loading report…</p>
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-card-value">R{Number(report.totalRevenue || 0).toFixed(2)}</div>
              <div className="stat-card-label">Total revenue</div>
            </div>
            <div className="stat-card">
              <div className="stat-card-value">{report.completedOrders}</div>
              <div className="stat-card-label">Completed orders</div>
            </div>
          </div>

          <div className="inv-section">
            <div className="inv-section-head">
              <h2>Activity overview</h2>
            </div>
            <div style={{ background: "var(--paper-raised)", border: "1px solid var(--border)", borderRadius: "14px", padding: "20px" }}>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="name" tick={{ fontSize: 12, fill: "#4b5d55" }} angle={-15} textAnchor="end" height={60} />
                  <YAxis tick={{ fontSize: 12, fill: "#4b5d55" }} allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="value" fill="#1f4b3f" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}
    </AppShell>
  );
}