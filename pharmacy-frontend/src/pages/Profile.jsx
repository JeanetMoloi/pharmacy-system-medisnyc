import { useEffect, useState } from "react";
import AppShell from "../components/AppShell";
import { Field, Alert } from "../components/Field";
import { getMyProfile, updateMyProfile } from "../api/authApi";
import { parseApiError } from "../utils/parseApiError";
import "../components/Form.css";
import "./Profile.css";

export default function Profile() {
    const [profile, setProfile] = useState(null);
    const [form, setForm] = useState({ firstName: "", lastName: "", email: "" });
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    useEffect(() => {
        let cancelled = false;
        getMyProfile()
            .then((data) => {
                if (cancelled) return;
                setProfile(data);
                setForm({ firstName: data.firstName || "", lastName: data.lastName || "", email: data.email || "" });
            })
            .catch((err) => setError(parseApiError(err).message))
            .finally(() => setLoading(false));
        return () => {
            cancelled = true;
        };
    }, []);

    function update(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSuccess("");
        setSaving(true);
        try {
            const updated = await updateMyProfile(form);
            setProfile(updated);
            setSuccess("Profile updated.");
        } catch (err) {
            setError(parseApiError(err).message);
        } finally {
            setSaving(false);
        }
    }

    return (
        <AppShell>
            <div className="profile-header">
                <div>
                    <div className="profile-eyebrow">Account</div>
                    <h1>Your profile</h1>
                </div>
                {profile && (
                    <div className="profile-roles">
                        {profile.roles?.map((r) => (
                            <span key={r.name} className="profile-role-chip">
                {r.name.replace(/^ROLE_/, "")}
              </span>
                        ))}
                    </div>
                )}
            </div>

            {loading ? (
                <p className="profile-loading">Loading your profile…</p>
            ) : (
                <div className="profile-card">
                    <form onSubmit={handleSubmit} noValidate>
                        <Alert type="error">{error}</Alert>
                        <Alert type="success">{success}</Alert>

                        <div className="field">
                            <label className="field-label">Username</label>
                            <input className="field-input" value={profile?.username || ""} disabled />
                            <span className="helper-text" style={{ marginBottom: 0 }}>
                Usernames can't be changed.
              </span>
                        </div>

                        <div className="field">
                            <label className="field-label">Linked profile ID</label>
                            <input
                                className="field-input"
                                value={profile?.linkedProfileId || "(not linked)"}
                                disabled
                            />
                            <span className="helper-text" style={{ marginBottom: 0 }}>
                Must match your Doctor or Patient ID for appointments to show up. Ask an admin to set this
                in User Management if it's missing.
              </span>
                        </div>

                        <div className="field-row">
                            <Field
                                id="firstName"
                                label="First name"
                                value={form.firstName}
                                onChange={(e) => update("firstName", e.target.value)}
                            />
                            <Field
                                id="lastName"
                                label="Last name"
                                value={form.lastName}
                                onChange={(e) => update("lastName", e.target.value)}
                            />
                        </div>

                        <Field
                            id="email"
                            label="Email"
                            type="email"
                            value={form.email}
                            onChange={(e) => update("email", e.target.value)}
                        />

                        <button className="btn-primary" type="submit" disabled={saving} style={{ maxWidth: "220px" }}>
                            {saving ? "Saving…" : "Save changes"}
                        </button>
                    </form>
                </div>
            )}
        </AppShell>
    );
}