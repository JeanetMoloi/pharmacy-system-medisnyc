package za.ac.cput.medisnyc.util;

import java.util.List;
import java.util.Optional;

public final class SpecializationCatalog {

    private static final List<String> SPECIALIZATIONS = List.of(
            "General Practitioner",
            "Pediatrician",
            "Dentist",
            "Cardiologist",
            "Dermatologist",
            "Gynecologist",
            "Orthopedic Surgeon",
            "ENT Specialist",
            "Ophthalmologist",
            "Neurologist",
            "Psychiatrist",
            "Physician (Internal Medicine)",
            "Urologist",
            "Endocrinologist"
    );

    private SpecializationCatalog() {
    }

    public static List<String> getSpecializations() {
        return SPECIALIZATIONS;
    }

    public static Optional<String> canonicalize(String specialization) {
        if (specialization == null) {
            return Optional.empty();
        }
        return SPECIALIZATIONS.stream()
                .filter(value -> value.equalsIgnoreCase(specialization.trim()))
                .findFirst();
    }
}
