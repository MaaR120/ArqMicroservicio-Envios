package com.envios.envios.Controller;

import com.envios.envios.Entities.Shipment;
import com.envios.envios.Entities.ShipmentHistory;
import com.envios.envios.Service.ShipmentService;
import com.envios.envios.DTO.ShipmentUpdateDTO; // <-- Importar el DTO
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    // GET /shipments/{id} (Perfecto, sin cambios)
    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipmentById(@PathVariable String id) {
        Optional<Shipment> shipment = shipmentService.getShipmentById(id);
        return shipment.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    // GET /shipments/order/{orderId} (Perfecto, sin cambios)
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Shipment> getShipmentByOrderId(@PathVariable String orderId) {
        Optional<Shipment> shipment = shipmentService.getShipmentByOrderId(orderId);
        return shipment.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    // GET /shipments/{id}/history  <-- NUEVO ENDPOINT
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ShipmentHistory>> getHistory(@PathVariable String id) {
        try {
            List<ShipmentHistory> history = shipmentService.getShipmentHistory(id);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // --- ENDPOINT PATCH CORREGIDO ---

    // PATCH /shipments/{id}
    @PatchMapping("/{id}")
    public ResponseEntity<Shipment> updateShipment(
            @PathVariable String id,
            @RequestBody ShipmentUpdateDTO updateDTO) { // <-- Usamos @RequestBody con el DTO
        
        // El servicio ya maneja los nulls, así que pasamos los valores directamente
        Shipment updatedShipment = shipmentService.updateShipment(
            id, 
            updateDTO.getEstado(), 
            updateDTO.getTransportista()
        );
        
        return ResponseEntity.ok(updatedShipment);
    }

    // --- MANEJADOR DE EXCEPCIONES ---

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<String> handleShipmentNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}