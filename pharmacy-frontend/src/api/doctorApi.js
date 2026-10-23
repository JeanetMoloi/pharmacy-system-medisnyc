import client from "./client";

export function getDoctors(specialization) {
    return client
        .get("/api/doctors", { params: specialization ? { specialization } : {} })
        .then((res) => res.data);
}

export function getAllDoctors() {
    return getDoctors();
}

export function getSpecializations() {
    return client.get("/api/doctors/specializations").then((res) => res.data);
}

export function getDoctorById(doctorId) {
    return client.get(`/api/doctors/${doctorId}`).then((res) => res.data);
}

export function createDoctor({ firstName, lastName, specialization, phoneNumber, email }) {
    return client
        .post("/api/doctors", { firstName, lastName, specialization, phoneNumber, email })
        .then((res) => res.data);
}

export function updateDoctor(doctorId, { firstName, lastName, specialization, phoneNumber, email }) {
    return client
        .put(`/api/doctors/${doctorId}`, { firstName, lastName, specialization, phoneNumber, email })
        .then((res) => res.data);
}

export function deleteDoctor(doctorId) {
    return client.delete(`/api/doctors/${doctorId}`);
}
