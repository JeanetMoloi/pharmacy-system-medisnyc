
import { useState } from "react";
import PublicLayout from "../components/PublicLayout";
import { Field, Alert } from "../components/Field";
import "../components/PublicLayout.css";
import "../components/Form.css";

export default function Contact() {
    const [form, setForm] = useState({ name: "", email: "", message: "" });
    const [sent, setSent] = useState(false);

    function update(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        // No contact-form endpoint exists on the backend yet — this confirms locally.
        // Wire this up to a real endpoint (e.g. POST /api/contact) if one gets added.
        setSent(true);
    }

    return (
        <PublicLayout>
            <section className="pub-section" style={{ paddingTop: "56px" }}>
                <div className="pub-eyebrow">Contact us</div>
                <h1 style={{ maxWidth: "20ch" }}>Questions about MediSync?</h1>
            </section>

            <section className="pub-section" style={{ paddingTop: 0 }}>
                <div className="pub-contact-grid">
                    <div>
                        <div className="pub-contact-card">
                            <h4>Email</h4>
                            <p>support@medisync.example</p>
                        </div>
                        <div className="pub-contact-card">
                            <h4>Phone</h4>
                            <p>+27 21 000 0000</p>
                        </div>
                        <div className="pub-contact-card">
                            <h4>Address</h4>
                            <p>Cape Peninsula University of Technology, Cape Town, South Africa</p>
                        </div>
                    </div>

                    <div className="pub-card">
                        {sent ? (
                            <Alert type="success">Thanks — your message has been noted. We'll get back to you soon.</Alert>
                        ) : (
                            <form onSubmit={handleSubmit} noValidate>
                                <Field id="name" label="Name" value={form.name} onChange={(e) => update("name", e.target.value)} required />
                                <Field
                                    id="email"
                                    label="Email"
                                    type="email"
                                    value={form.email}
                                    onChange={(e) => update("email", e.target.value)}
                                    required
                                />
                                <div className="field">
                                    <label className="field-label" htmlFor="message">
                                        Message
                                    </label>
                                    <textarea
                                        id="message"
                                        className="field-input"
                                        value={form.message}
                                        onChange={(e) => update("message", e.target.value)}
                                        required
                                    />
                                </div>
                                <button className="btn-primary" type="submit">
                                    Send message
                                </button>
                            </form>
                        )}
                    </div>
                </div>
            </section>
        </PublicLayout>
    );
}