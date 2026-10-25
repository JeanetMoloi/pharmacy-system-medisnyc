package za.ac.cput.medisnyc.controller;

import jakarta.validation.Valid;
import za.ac.cput.medisnyc.domain.Medication;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.dto.CreateMedicationRequest;
import za.ac.cput.medisnyc.dto.UpdateMedicationRequest;
import za.ac.cput.medisnyc.service.MedicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    public ResponseEntity<List<Medication>> getAll() {
        return ResponseEntity.ok(medicationService.getAll());
    }

    @GetMapping("/{medicationId}")
    public ResponseEntity<Medication> getById(@PathVariable String medicationId) {
        return ResponseEntity.ok(medicationService.getById(medicationId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Medication>> search(@RequestParam String name) {
        return ResponseEntity.ok(medicationService.search(name));
    }

    @PostMapping
    public ResponseEntity<Medication> addMedicine(@Valid @RequestBody CreateMedicationRequest request,
                                                   @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(medicationService.addMedicine(request, actor.getUsername()));
    }

    @PutMapping("/{medicationId}")
    public ResponseEntity<Medication> updateMedicine(@PathVariable String medicationId,
                                                      @Valid @RequestBody UpdateMedicationRequest request,
                                                      @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(medicationService.updateMedicine(
                medicationId, request, actor.getUsername()));
    }

    @DeleteMapping("/{medicationId}")
    public ResponseEntity<Void> deleteMedicine(@PathVariable String medicationId,
                                                @AuthenticationPrincipal User actor) {
        medicationService.deleteMedicine(medicationId, actor.getUsername());
        return ResponseEntity.noContent().build();
    }
}
