

import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { Field, Alert } from "../components/Field";
import { resetPassword } from "../api/authApi";
import { parseApiError } from "../utils/parseApiError";
import "../components/Form.css";

export default function ResetPassword() {
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({
    token: location.state?.token || "",
    newPassword: "",
    confirmPassword: "",
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (form.newPassword !== form.confirmPassword) {
      setError("Passwords don't match.");
      return;
    }

    setSubmitting(true);
    try {
      await resetPassword({ token: form.token, newPassword: form.newPassword });
      setSuccess("Password reset. Redirecting you to sign in…");
      setTimeout(() => navigate("/login"), 1500);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      eyebrow="Account access"
      title="Reset your password"
      subtitle={
        location.state?.email
          ? `We generated a token for ${location.state.email}. Set a new password below.`
          : "Paste the reset token you were given, then set a new password."
      }
      footer={
        <>
          <Link to="/login">Back to sign in</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate>
        <Alert type="error">{error}</Alert>
        <Alert type="success">{success}</Alert>

        <Field
          id="token"
          label="Reset token"
          value={form.token}
          onChange={(e) => update("token", e.target.value)}
          required
        />

        <Field
          id="newPassword"
          label="New password"
          type="password"
          value={form.newPassword}
          onChange={(e) => update("newPassword", e.target.value)}
          required
        />

        <Field
          id="confirmPassword"
          label="Confirm new password"
          type="password"
          value={form.confirmPassword}
          onChange={(e) => update("confirmPassword", e.target.value)}
          required
        />

        <button className="btn-primary" type="submit" disabled={submitting}>
          {submitting ? "Resetting…" : "Reset password"}
        </button>
      </form>
    </AuthLayout>
  );
}