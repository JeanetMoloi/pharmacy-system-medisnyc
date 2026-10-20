import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { Field, Alert } from "../components/Field";
import { forgotPassword } from "../api/authApi";
import { parseApiError } from "../utils/parseApiError";
import "../components/Form.css";

export default function ForgotPassword() {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSubmitting(true);
        try {
            const res = await forgotPassword({ email });
            // Demo backend returns the token directly in the message instead of emailing it.
            // We hand it straight to the reset page so the flow is testable end to end.
            const match = res.message?.match(/Reset token generated: (.+)$/);
            const token = match ? match[1] : "";
            navigate("/reset-password", { state: { email, token } });
        } catch (err) {
            setError(parseApiError(err).message);
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <AuthLayout
            eyebrow="Account access"
            title="Forgot your password?"
            subtitle="Enter the email on your account and we'll generate a reset token."
            footer={
                <>
                    Remembered it after all? <Link to="/login">Back to sign in</Link>
                </>
            }
        >
            <form onSubmit={handleSubmit} noValidate>
                <Alert type="error">{error}</Alert>

                <Field
                    id="email"
                    label="Email"
                    type="email"
                    autoComplete="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                />

                <button className="btn-primary" type="submit" disabled={submitting}>
                    {submitting ? "Sending…" : "Send reset token"}
                </button>
            </form>
        </AuthLayout>
    );
}