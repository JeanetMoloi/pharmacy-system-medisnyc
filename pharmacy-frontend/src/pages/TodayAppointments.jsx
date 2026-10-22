

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import AppShell from "../components/AppShell";
import ConsultNav from "../components/ConsultNav";
import { Alert } from "../components/Field";
import { useAuth } from "../context/AuthContext";
import * as appointmentApi from "../api/appointmentApi";
import { parseApiError } from "../utils/parseApiError";
import "./Appointments.css";
import "./Consultations.css";

function isToday(iso) {
  if (!iso) return false;
  const d = new Date(iso);
  const now = new Date();
  return (
    d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate()
  );
}

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}

export default function TodaysAppointments() {
  const { profileId } = useAuth();
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showAll, setShowAll] = useState(false);

  useEffect(() => {
    appointmentApi
      .getAppointmentsByDoctor(profileId())
      .then((data) => {
        const sorted = [...data].sort((a, b) => new Date(a.appointmentDate) - new Date(b.appointmentDate));
        setAppointments(sorted);
      })
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }, [profileId]);

  const visible = showAll ? appointments : appointments.filter((a) => isToday(a.appointmentDate));

  return (
    <AppShell>
      <div className="appts-header">
        <div>
          <div className="appts-eyebrow">Module 3</div>
          <h1>Today's appointments</h1>
          <p className="appts-sub">Start a consultation straight from a booked appointment.</p>
        </div>
        <button className="btn-secondary consult-toggle-btn" onClick={() => setShowAll((v) => !v)}>
          {showAll ? "Show today only" : "Show all upcoming"}
        </button>
      </div>

      <ConsultNav />

      <Alert type="error">{error}</Alert>

      {loading ? (
        <p className="appts-loading">Loading appointments…</p>
      ) : visible.length === 0 ? (
        <div className="appts-empty">
          <p>{showAll ? "No appointments booked with you yet." : "Nothing on today's list."}</p>
        </div>
      ) : (
        <div className="appts-list">
          {visible.map((a) => (
            <div className="appts-row" key={a.appointmentId}>
              <div className="appts-row-main">
                <div className="appts-row-id">{formatTime(a.appointmentDate)}</div>
                <div className="appts-row-parties">Patient: {a.patientId}</div>
                {a.reason && <div className="appts-row-reason">{a.reason}</div>}
              </div>
              <div className="appts-row-side">
                <Link
                  to={`/consultations/new?appointmentId=${a.appointmentId}&patientId=${a.patientId}`}
                  className="btn-primary consult-start-btn"
                >
                  Start consultation
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </AppShell>
  );
}