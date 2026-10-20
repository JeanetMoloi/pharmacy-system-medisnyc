consultNav.jsx
import { NavLink } from "react-router-dom";
import "../pages/Admin.css";

const LINKS = [
    { to: "/consultations", label: "Today's appointments", end: true },
    { to: "/consultations/patients", label: "Your patients" },
];

export default function ConsultNav() {
    return (
        <nav className="admin-nav">
            {LINKS.map((link) => (
                <NavLink
                    key={link.to}
                    to={link.to}
                    end={link.end}
                    className={({ isActive }) => `admin-nav-link ${isActive ? "active" : ""}`}
                >
                    {link.label}
                </NavLink>
            ))}
        </nav>
    );
}