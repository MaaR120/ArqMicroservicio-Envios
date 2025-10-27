package com.envios.envios.Service;

import com.envios.envios.Entities.Shipment;
import com.envios.envios.Enum.EstadoShipment;
import com.envios.envios.Enum.Transportista;
import com.envios.envios.Repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

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

        return shipmentRepository.save(shipment);
    }

    public Optional<Shipment> getShipmentById(String id) {
        return shipmentRepository.findById(id);
    }

    public Optional<Shipment> getShipmentByOrderId(String orderId) {
        return Optional.ofNullable(shipmentRepository.findByOrderId(orderId));
    }

public Shipment updateShipment(String id, EstadoShipment newStatus, Transportista newTransportista) {
    Shipment shipment = shipmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Shipment not found"));

    if (newStatus != null) {
        shipment.setEstado(newStatus);
    }

    if (newTransportista != null) {
        shipment.setTransportista(newTransportista);
    }

    return shipmentRepository.save(shipment);
}
}
