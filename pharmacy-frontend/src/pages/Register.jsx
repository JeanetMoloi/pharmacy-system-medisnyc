

import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { Field, Alert } from "../components/Field";
import * as authApi from "../api/authApi";
import { parseApiError } from "../utils/parseApiError";
import "../components/Form.css";

const EMPTY_FORM = {
  firstName: "",
  lastName: "",
  username: "",
  email: "",
  password: "",
  role: "PATIENT",
  phoneNumber: "",
  dateOfBirth: "",
};

export default function Register() {
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setFieldErrors({});
    setSubmitting(true);
    try {
      // The backend returns a JWT on register (auto-login), but we deliberately
      // don't store it here — the account is created, and the person signs in
      // separately on the login page.
      await authApi.register(form);
      navigate("/login", { replace: true, state: { justRegistered: true, username: form.username } });
    } catch (err) {
      const parsed = parseApiError(err);
      setError(parsed.message);
      setFieldErrors(parsed.fieldErrors);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      eyebrow="Account access"
      title="Create your patient account"
      subtitle="Doctor and admin accounts are set up by an administrator, not self-registered."
      footer={
        <>
          Already have an account? <Link to="/login">Sign in</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate>
        <Alert type="error">{error}</Alert>

        <div className="field-row">
          <Field
            id="firstName"
            label="First name"
            value={form.firstName}
            onChange={(e) => update("firstName", e.target.value)}
            error={fieldErrors.firstName}
            required
          />
          <Field
            id="lastName"
            label="Last name"
            value={form.lastName}
            onChange={(e) => update("lastName", e.target.value)}
            error={fieldErrors.lastName}
            required
          />
        </div>

        <Field
          id="username"
          label="Username"
          value={form.username}
          onChange={(e) => update("username", e.target.value)}
          error={fieldErrors.username}
          required
        />

        <Field
          id="email"
          label="Email"
          type="email"
          value={form.email}
          onChange={(e) => update("email", e.target.value)}
          error={fieldErrors.email}
          required
        />

        <Field
          id="password"
          label="Password"
          type="password"
          value={form.password}
          onChange={(e) => update("password", e.target.value)}
          error={fieldErrors.password}
          required
        />
        {!fieldErrors.password && <div className="helper-text">At least 8 characters.</div>}

        <div className="field-row">
          <Field
            id="phoneNumber"
            label="Phone number"
            value={form.phoneNumber}
            onChange={(e) => update("phoneNumber", e.target.value)}
            error={fieldErrors.phoneNumber}
            placeholder="e.g. 0821234567"
            required
          />
          <Field
            id="dateOfBirth"
            label="Date of birth"
            type="date"
            value={form.dateOfBirth}
            onChange={(e) => update("dateOfBirth", e.target.value)}
            error={fieldErrors.dateOfBirth}
            required
          />
        </div>
        <div className="helper-text" style={{ marginTop: "-10px" }}>
          We'll set up your patient record automatically using these details.
        </div>

        <button className="btn-primary" type="submit" disabled={submitting}>
          {submitting ? "Creating account…" : "Create account"}
        </button>
      </form>
    </AuthLayout>
  );
}