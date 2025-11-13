package com.envios.envios.Service;

import com.envios.envios.Entities.Shipment;
import com.envios.envios.Entities.ShipmentHistory; 
import com.envios.envios.Enum.EstadoShipment;
import com.envios.envios.Enum.Transportista;
import com.envios.envios.Repository.ShipmentHistoryRepository; 
import com.envios.envios.Repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentHistoryRepository historyRepository;

    public ShipmentService(ShipmentRepository shipmentRepository, ShipmentHistoryRepository historyRepository) {
        this.shipmentRepository = shipmentRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public Shipment createShipment(String orderId, String direccion) {

        Optional<Shipment> existingShipment = this.getShipmentByOrderId(orderId);
        
        if (existingShipment.isPresent()) {
            // Si ya existe, retornamos el envío existente y no creamos uno nuevo.
            System.out.println("Mensaje duplicado recibido para orderId: " + orderId + ". No se crea nuevo envío.");
            return existingShipment.get();
        }
        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .direccion(direccion)
                .transportista(Transportista.En_Decision) // fijo por ahora, se puede parametrizar
                .estado(EstadoShipment.En_Preparacion)                  // estado inicial
                .trackingCode(UUID.randomUUID().toString()) // tracking aleatorio
                .costo(1500.0)                     // costo fijo
                .build();
        
        Shipment savedShipment = shipmentRepository.save(shipment);

        // GUARDAR HISTORIAL INICIAL
        this.saveHistory(savedShipment.getId(), EstadoShipment.En_Preparacion, "Creación automática por Orden");

        return savedShipment;
    }

    private void saveHistory(String shipmentId, EstadoShipment estado, String observacion) {
        ShipmentHistory history = ShipmentHistory.builder()
                .shipmentId(shipmentId)
                .estado(estado)
                .fecha(LocalDateTime.now())
                .operador(observacion)
                .build();
        historyRepository.save(history);
    }

    public Optional<Shipment> getShipmentById(String id) {
        return shipmentRepository.findById(id);
    }

    public Optional<Shipment> getShipmentByOrderId(String orderId) {
        return Optional.ofNullable(shipmentRepository.findByOrderId(orderId));
    }

    @Transactional
    public Shipment updateShipment(String id, EstadoShipment newStatus, Transportista newTransportista) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (newStatus != null && newStatus != shipment.getEstado()) { // Solo si cambia el estado
                shipment.setEstado(newStatus);
                // GUARDAR HISTORIAL DE CAMBIO
                this.saveHistory(id, newStatus, "Actualización de estado");
            }

        if (newTransportista != null) {
            shipment.setTransportista(newTransportista);
        }

        return shipmentRepository.save(shipment);
    }
    public List<ShipmentHistory> getShipmentHistory(String shipmentId) {
        // Verificamos que el envío exista primero (opcional, pero buena práctica)
        if (!shipmentRepository.existsById(shipmentId)) {
            throw new RuntimeException("Shipment not found");
        }
        return historyRepository.findByShipmentIdOrderByFechaAsc(shipmentId);
    }
}
