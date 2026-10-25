package za.ac.cput.medisnyc.controller;

import jakarta.validation.Valid;
import za.ac.cput.medisnyc.domain.Inventory;
import za.ac.cput.medisnyc.domain.User;
import za.ac.cput.medisnyc.dto.AdjustStockRequest;
import za.ac.cput.medisnyc.dto.InventorySummary;
import za.ac.cput.medisnyc.dto.ReceiveStockRequest;
import za.ac.cput.medisnyc.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<Inventory>> getAll() {
        return ResponseEntity.ok(inventoryService.getAll());
    }

    @GetMapping("/summary")
    public ResponseEntity<List<InventorySummary>> getSummary() {
        return ResponseEntity.ok(inventoryService.getSummary());
    }

    @GetMapping("/{inventoryId}")
    public ResponseEntity<Inventory> getById(@PathVariable String inventoryId) {
        return ResponseEntity.ok(inventoryService.getById(inventoryId));
    }

    @GetMapping("/medication/{medicationId}")
    public ResponseEntity<List<Inventory>> getByMedication(@PathVariable String medicationId) {
        return ResponseEntity.ok(inventoryService.getByMedication(medicationId));
    }

    @PostMapping
    public ResponseEntity<Inventory> receiveStock(@Valid @RequestBody ReceiveStockRequest request,
                                                   @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(inventoryService.receiveStock(request, actor.getUsername()));
    }

    @PutMapping("/{inventoryId}/adjust")
    public ResponseEntity<Inventory> adjustStock(@PathVariable String inventoryId,
                                                  @Valid @RequestBody AdjustStockRequest request,
                                                  @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(inventoryService.adjustStock(
                inventoryId, request, actor.getUsername()));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Inventory>> lowStockAlerts() {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts());
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<List<Inventory>> expiringSoon(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(inventoryService.getExpiringSoon(days));
    }

    @GetMapping("/expired")
    public ResponseEntity<List<Inventory>> expiredStock() {
        return ResponseEntity.ok(inventoryService.getExpiredStock());
    }
}
