
notification.js
import client from "./client";

// Module 5 — Notify Patient API. Maps onto NotificationController (/api/notifications).

export function getForPatient(patientId) {
    return client.get(`/api/notifications/patient/${patientId}`).then((res) => res.data);
}

export function getUnread(patientId) {
    return client.get(`/api/notifications/patient/${patientId}/unread`).then((res) => res.data);
}

export function markAsRead(notificationId) {
    return client.put(`/api/notifications/${notificationId}/read`).then((res) => res.data);
}