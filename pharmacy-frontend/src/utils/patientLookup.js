import * as patientApi from "../api/patientApi";

const patientLookupCache = new Map();

export function getPatientDetails(patientId) {
    if (!patientId) return Promise.resolve(null);
    if (!patientLookupCache.has(patientId)) {
        patientLookupCache.set(
            patientId,
            patientApi.getPatientById(patientId).catch(() => null),
        );
    }
    return patientLookupCache.get(patientId);
}

export function formatPatientName(patient, fallbackId) {
    if (!patient) return fallbackId;
    const name = [patient.firstName, patient.lastName].filter(Boolean).join(" ");
    const dateOfBirth = patient.dateOfBirth
        ? new Date(`${patient.dateOfBirth}T00:00:00`).toLocaleDateString()
        : "";
    return dateOfBirth
        ? `${name || fallbackId} · DOB: ${dateOfBirth}`
        : name || fallbackId;
}
