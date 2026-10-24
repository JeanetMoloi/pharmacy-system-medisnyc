package za.ac.cput.medisnyc.service;

import za.ac.cput.medisnyc.domain.User;

public interface DoctorLoginCreator {
    User createDoctorLogin(String doctorId, String username, String email, String password);
}
