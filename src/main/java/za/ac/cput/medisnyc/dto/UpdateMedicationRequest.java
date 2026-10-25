package za.ac.cput.medisnyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateMedicationRequest(
        @NotBlank(message = "Medication name is required")
        @Size(max = 100, message = "Medication name must be at most 100 characters")
        String medicationName,
        @NotBlank(message = "Strength is required") String strength,
        @NotBlank(message = "Dosage form is required") String dosageForm,
        @NotBlank(message = "Category is required") String category,
        String manufacturer,
        String description
) {
}
