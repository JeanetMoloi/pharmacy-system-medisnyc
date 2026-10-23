package za.ac.cput.medisnyc.service;

/* DoctorService.java
   Module 2: doctor directory - lookup for the patient-facing booking flow,
   and creation for admins adding a doctor to the practice.
   Author: Lisakhanya Mpahla
*/

import za.ac.cput.medisnyc.domain.Doctor;
import za.ac.cput.medisnyc.factory.DoctorFactory;
import za.ac.cput.medisnyc.repository.jpa.DoctorJpaRepository;
import za.ac.cput.medisnyc.util.Helper;
import za.ac.cput.medisnyc.util.SpecializationCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DoctorService {

    private final DoctorJpaRepository doctorRepository;

    @Autowired
    public DoctorService(DoctorJpaRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public List<Doctor> getAll() {
        return doctorRepository.findAll();
    }

    public List<Doctor> getBySpecialization(String specialization) {
        return doctorRepository.findBySpecializationIgnoreCase(
                canonicalSpecialization(specialization));
    }

    public Doctor getById(String doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));
    }

    public Doctor create(String firstName, String lastName, String specialization,
                         String phoneNumber, String email) {
        String canonicalSpecialization = canonicalSpecialization(specialization);
        String doctorId = "DOC-" + UUID.randomUUID();
        Doctor doctor = DoctorFactory.createDoctor(
                doctorId, firstName, lastName, canonicalSpecialization, phoneNumber, email);
        if (doctor == null) {
            throw new IllegalArgumentException(
                    "Couldn't create doctor - check first/last name, specialization, phone number and email are all valid.");
        }
        return doctorRepository.save(doctor);
    }

    public Doctor update(String doctorId, String firstName, String lastName, String specialization,
                         String phoneNumber, String email) {
        Doctor existing = getById(doctorId);
        String canonicalSpecialization = canonicalSpecialization(specialization);
        Doctor updated = DoctorFactory.createDoctor(
                existing.getDoctorId(), firstName, lastName, canonicalSpecialization, phoneNumber, email);
        if (updated == null) {
            throw new IllegalArgumentException(
                    "Couldn't update doctor - check first/last name, phone number and email are all valid.");
        }
        return doctorRepository.save(updated);
    }

    private String canonicalSpecialization(String specialization) {
        return SpecializationCatalog.canonicalize(specialization)
                .orElseThrow(() -> new IllegalArgumentException("Please choose a valid specialization"));
    }

    public void delete(String doctorId) {
        getById(doctorId);
        doctorRepository.deleteById(doctorId);
    }
}
