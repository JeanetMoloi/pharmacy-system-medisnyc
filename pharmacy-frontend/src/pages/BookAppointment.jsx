BookAppointment.jsx

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppShell from "../components/AppShell";
import { Field, Alert } from "../components/Field";
import { useAuth } from "../context/AuthContext";
import * as appointmentApi from "../api/appointmentApi";
import * as doctorApi from "../api/doctorApi";
import { parseApiError } from "../utils/parseApiError";
import "../components/Form.css";
import "./Appointments.css";

export default function BookAppointment() {
    const { profileId } = useAuth();
    const navigate = useNavigate();

    const [doctors, setDoctors] = useState([]);
    const [loadingDoctors, setLoadingDoctors] = useState(true);
    const [doctorsError, setDoctorsError] = useState("");

    const [form, setForm] = useState({ doctorId: "", appointmentDate: "", reason: "", notes: "" });
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        doctorApi
            .getAllDoctors()
            .then(setDoctors)
            .catch((err) => setDoctorsError(parseApiError(err).message))
            .finally(() => setLoadingDoctors(false));
    }, []);

    function update(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");

        if (!form.doctorId) {
            setError("Choose a doctor to book with.");
            return;
        }
        if (!form.appointmentDate) {
            setError("Choose a date and time.");
            return;
        }

        setSubmitting(true);
        try {
            await appointmentApi.bookAppointment({
                patientId: profileId(),
                doctorId: form.doctorId,
                appointmentDate: form.appointmentDate,
                reason: form.reason,
                notes: form.notes,
            });
            navigate("/appointments", { replace: true });
        } catch (err) {
            setError(parseApiError(err).message);
        } finally {
            setSubmitting(false);
        }
    }

    // Prevent picking a time in the past.
    const minDateTime = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
        .toISOString()
        .slice(0, 16);

    return (
        <AppShell>
            <div className="appts-eyebrow">Module 2</div>
            <h1>Book an appointment</h1>
            <p className="appts-sub">Pick a doctor, a time, and let them know why you're coming in.</p>

            <div className="book-card" style={{ marginTop: "24px" }}>
                <form onSubmit={handleSubmit} noValidate>
                    <Alert type="error">{error || doctorsError}</Alert>

                    <label className="field-label" style={{ display: "block", marginBottom: "10px" }}>
                        Doctor
                    </label>
                    {loadingDoctors ? (
                        <p className="doctor-empty">Loading doctors…</p>
                    ) : doctors.length === 0 ? (
                        <p className="doctor-empty">
                            No doctors are registered yet — an admin needs to add one before appointments can be booked.
                        </p>
                    ) : (
                        <div className="doctor-picker">
                            {doctors.map((doc) => (
                                <div
                                    key={doc.doctorId}
                                    className={`doctor-option ${form.doctorId === doc.doctorId ? "selected" : ""}`}
                                    onClick={() => update("doctorId", doc.doctorId)}
                                >
                                    <div>
                                        <div className="doctor-option-name">
                                            Dr. {doc.firstName} {doc.lastName}
                                        </div>
                                        {doc.specialization && <div className="doctor-option-spec">{doc.specialization}</div>}
                                    </div>
                                    <span className="doctor-option-radio" />
                                </div>
                            ))}
                        </div>
                    )}

                    <Field
                        id="appointmentDate"
                        label="Date & time"
                        type="datetime-local"
                        min={minDateTime}
                        value={form.appointmentDate}
                        onChange={(e) => update("appointmentDate", e.target.value)}
                        required
                    />

                    <Field
                        id="reason"
                        label="Reason for visit"
                        value={form.reason}
                        onChange={(e) => update("reason", e.target.value)}
                        placeholder="e.g. Follow-up consultation"
                    />

                    <Field
                        id="notes"
                        label="Notes (optional)"
                        value={form.notes}
                        onChange={(e) => update("notes", e.target.value)}
                        placeholder="Anything else the doctor should know"
                    />

                    <button className="btn-primary" type="submit" disabled={submitting}>
                        {submitting ? "Booking…" : "Book appointment"}
                    </button>
                </form>
            </div>
        </AppShell>
    );
}