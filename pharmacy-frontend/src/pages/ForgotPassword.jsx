import { useState } from "react";
import { Link } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { Field, Alert } from "../components/Field";
import { forgotPassword } from "../api/authApi";
import { parseApiError } from "../utils/parseApiError";
import "../components/Form.css";

export default function ForgotPassword() {
    const [email, setEmail] = useState("");
    const [error, setError] = useState("");
    const [submitted, setSubmitted] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSubmitting(true);
        try {
            await forgotPassword({ email });
            setSubmitted(true);
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
            subtitle="Enter your account email and, if it matches an account, reset instructions will be sent."
            footer={
                <>
                    Remembered it after all? <Link to="/login">Back to sign in</Link>
                </>
            }
        >
            <form onSubmit={handleSubmit} noValidate>
                <Alert type="error">{error}</Alert>
                {submitted && <p role="status">If an account exists for that email, password reset instructions will be sent.</p>}

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