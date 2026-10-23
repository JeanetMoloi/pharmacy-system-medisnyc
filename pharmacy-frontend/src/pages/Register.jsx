import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { Field, Alert } from "../components/Field";
import { useAuth } from "../context/useAuth";
import { parseApiError } from "../utils/parseApiError";
import "../components/Form.css";

const EMPTY_FORM = {
  firstName: "",
  lastName: "",
  username: "",
  email: "",
  password: "",
  phoneNumber: "",
  dateOfBirth: "",
  confirmPassword: "",
};

export default function Register() {
  const { register } = useAuth();
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
    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match.");
      setFieldErrors({ confirmPassword: "Passwords do not match." });
      return;
    }
    setSubmitting(true);
    try {
      await register({
        firstName: form.firstName,
        lastName: form.lastName,
        username: form.username,
        email: form.email,
        phoneNumber: form.phoneNumber,
        dateOfBirth: form.dateOfBirth,
        password: form.password,
        role: "PATIENT",
      });
      navigate("/dashboard", { replace: true });
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
      subtitle="Create a patient account to book appointments and track prescriptions."
      footer={
        <>
          Already have an account? <Link to="/login">Sign in</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate>
        <Alert type="error">{error}</Alert>

        <Field
          id="firstName"
          label="First name"
          autoComplete="given-name"
          placeholder="Your first name"
          value={form.firstName}
          onChange={(e) => update("firstName", e.target.value)}
          error={fieldErrors.firstName}
          required
        />
        <Field
          id="lastName"
          label="Last name"
          autoComplete="family-name"
          placeholder="Your last name"
          value={form.lastName}
          onChange={(e) => update("lastName", e.target.value)}
          error={fieldErrors.lastName}
          required
        />

        <Field
          id="username"
          label="Username"
          autoComplete="username"
          placeholder="Choose a username"
          value={form.username}
          onChange={(e) => update("username", e.target.value)}
          error={fieldErrors.username}
          required
        />

        <Field
          id="email"
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          value={form.email}
          onChange={(e) => update("email", e.target.value)}
          error={fieldErrors.email}
          required
        />

        <Field
          id="password"
          label="Password"
          type="password"
          autoComplete="new-password"
          placeholder="At least 8 characters"
          minLength={8}
          value={form.password}
          onChange={(e) => update("password", e.target.value)}
          error={fieldErrors.password}
          required
        />
        {!fieldErrors.password && <div className="helper-text">Minimum 8 characters.</div>}

        <Field
          id="confirmPassword"
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          placeholder="Enter your password again"
          value={form.confirmPassword}
          onChange={(e) => update("confirmPassword", e.target.value)}
          error={fieldErrors.confirmPassword}
          required
        />
        <Field
          id="phoneNumber"
          label="Phone number"
          type="tel"
          autoComplete="tel"
          placeholder="e.g. 0821234567"
          value={form.phoneNumber}
          onChange={(e) => update("phoneNumber", e.target.value)}
          error={fieldErrors.phoneNumber}
          required
        />
        <Field
          id="dateOfBirth"
          label="Date of birth"
          type="date"
          autoComplete="bday"
          value={form.dateOfBirth}
          onChange={(e) => update("dateOfBirth", e.target.value)}
          error={fieldErrors.dateOfBirth}
          required
        />

        <button className="btn-primary" type="submit" disabled={submitting}>
          {submitting ? "Creating account…" : "Create account"}
        </button>
      </form>
    </AuthLayout>
  );
}