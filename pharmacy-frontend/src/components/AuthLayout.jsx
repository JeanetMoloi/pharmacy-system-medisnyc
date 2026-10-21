AuthLayout.jsx
import "./AuthLayout.css";

const FLOW_STEPS = ["Pending", "Received", "Preparing", "Ready for Collection", "Collected"];

export default function AuthLayout({ eyebrow, title, subtitle, children, footer }) {
    return (
        <div className="auth-shell">
            <aside className="auth-brand">
                <div className="auth-brand-top">
                    <div className="auth-mark">M</div>
                    <div>
                        <div className="auth-brand-name">MediSync</div>
                        <div className="auth-brand-sub">Pharmacy Prescription Management System</div>
                    </div>
                </div>

                <div className="auth-flow">
                    <div className="auth-flow-label">Every script, tracked end to end</div>
                    <ol className="auth-flow-rail">
                        {FLOW_STEPS.map((step, i) => (
                            <li key={step} className="auth-flow-step">
                                <span className="auth-flow-dot" />
                                <span className="auth-flow-text">{step}</span>
                                {i === 0 && <span className="auth-flow-you">you are here</span>}
                            </li>
                        ))}
                    </ol>
                </div>

                <p className="auth-brand-foot">
                    One account, three roles — patients, doctors and admins each get their own
                    dashboard. Admins also run the pharmacy floor: inventory and prescription
                    processing.
                </p>
            </aside>

            <main className="auth-main">
                <div className="auth-card">
                    <div className="auth-card-stub">
                        <span>MEDISYNC · CPUT</span>
                        <span>ACCOUNT ACCESS</span>
                    </div>
                    <div className="auth-card-body">
                        {eyebrow && <div className="auth-eyebrow">{eyebrow}</div>}
                        <h1 className="auth-title">{title}</h1>
                        {subtitle && <p className="auth-subtitle">{subtitle}</p>}
                        <div className="auth-form-area">{children}</div>
                        {footer && <div className="auth-footer">{footer}</div>}
                    </div>
                </div>
            </main>
        </div>
    );
}