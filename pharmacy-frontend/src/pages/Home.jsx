

import { Link } from "react-router-dom";
import PublicLayout from "../components/PublicLayout";
import "../components/PublicLayout.css";

const FEATURES = [
  { icon: "📅", title: "Appointment booking", body: "Patients pick a doctor and a time slot; doctors see their day at a glance." },
  { icon: "💊", title: "e-Prescriptions", body: "Doctors issue prescriptions straight from a consultation, medication by medication." },
  { icon: "📦", title: "Pharmacy inventory", body: "Stock levels, batches and expiry dates, with automatic low-stock alerts." },
  { icon: "🔔", title: "Real-time tracking", body: "Every prescription's status is visible to the patient, from pending to collected." },
];

const ROLES = [
  { title: "Patients", body: "Book appointments, message-free. Track every prescription from pending to ready for collection." },
  { title: "Doctors", body: "Run consultations, record diagnoses, and issue prescriptions in one flow." },
  { title: "Admins", body: "Run the pharmacy floor and the system — inventory, prescription processing, users and reports." },
];

const STEPS = [
  { title: "Register", body: "Create an account as a patient, doctor, or admin." },
  { title: "Book or consult", body: "Patients book with a doctor; doctors run the consultation." },
  { title: "Prescribe", body: "The doctor issues a prescription with the medications needed." },
  { title: "Track & collect", body: "The pharmacy fills it, and the patient watches it move to ready for collection." },
];

export default function Home() {
  return (
    <PublicLayout>
      <section className="pub-hero">
        <h1>Pharmacy prescriptions, from booking to collection, in one place</h1>
        <p>
          MediSync connects patients, doctors and pharmacy admins around a single prescription —
          no phone calls, no guessing whether it's ready yet.
        </p>
        <div className="pub-hero-actions">
          <Link to="/register" className="public-btn public-btn-primary">
            Get started
          </Link>
          <Link to="/login" className="public-btn public-btn-ghost">
            Login
          </Link>
        </div>
      </section>

      <section className="pub-section">
        <div className="pub-eyebrow">What it does</div>
        <div className="pub-grid-4">
          {FEATURES.map((f) => (
            <div className="pub-card" key={f.title}>
              <div className="pub-card-icon">{f.icon}</div>
              <h3>{f.title}</h3>
              <p>{f.body}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="pub-section" style={{ paddingTop: 0 }}>
        <div className="pub-eyebrow">Built for three roles</div>
        <div className="pub-grid-3">
          {ROLES.map((r) => (
            <div className="pub-card" key={r.title}>
              <h3>{r.title}</h3>
              <p>{r.body}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="pub-section" style={{ paddingTop: 0 }}>
        <div className="pub-eyebrow">How it works</div>
        <div className="pub-steps">
          {STEPS.map((s, i) => (
            <div className="pub-step" key={s.title}>
              <div className="pub-step-num">{String(i + 1).padStart(2, "0")}</div>
              <h3>{s.title}</h3>
              <p>{s.body}</p>
            </div>
          ))}
        </div>
      </section>

      <div className="pub-cta">
        <h2>Ready to get your prescriptions in order?</h2>
        <p>It takes a minute to register — no paperwork.</p>
        <Link to="/register" className="public-btn public-btn-primary">
          Create your account
        </Link>
      </div>
    </PublicLayout>
  );
}