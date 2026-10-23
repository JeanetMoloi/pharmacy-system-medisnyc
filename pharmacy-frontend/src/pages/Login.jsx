

import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { Field, Alert } from "../components/Field";
import { useAuth } from "../context/useAuth";
import { parseApiError } from "../utils/parseApiError";
import "../components/Form.css";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ username: location.state?.username || "", password: "" });
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
      await login(form);
      const redirectTo = location.state?.from || "/dashboard";
      navigate(redirectTo, { replace: true });
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
      title="Sign in to MediSync"
      subtitle="Enter your username and password to reach your dashboard."
      footer={
        <>
          New here? <Link to="/register">Create an account</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate>
        <Alert type="error">{error}</Alert>
        {location.state?.justRegistered && (
          <Alert type="success">Account created — sign in with your new username and password.</Alert>
        )}

        <Field
          id="username"
          label="Username"
          type="text"
          autoComplete="username"
          placeholder="Your username"
          value={form.username}
          onChange={(e) => update("username", e.target.value)}
          error={fieldErrors.username}
          required
        />

        <Field
          id="password"
          label="Password"
          type="password"
          autoComplete="current-password"
          placeholder="Your password"
          value={form.password}
          onChange={(e) => update("password", e.target.value)}
          error={fieldErrors.password}
          required
        />

        <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "-10px", marginBottom: "18px" }}>
          <Link className="auth-inline-link" to="/forgot-password">
            Forgot password?
          </Link>
        </div>

        <button className="btn-primary" type="submit" disabled={submitting}>
          {submitting ? "Signing in…" : "Sign in"}
        </button>
      </form>
      {import.meta.env.DEV && (
        <p className="helper-text" style={{ marginTop: "14px", marginBottom: 0 }}>
          Demo doctor login: firstname.lastname / password
        </p>
      )}
    </AuthLayout>
  );
}