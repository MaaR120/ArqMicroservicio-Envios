package com.envios.envios.Repository;


import com.envios.envios.Entities.Shipment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShipmentRepository extends MongoRepository<Shipment, String> {
    Shipment findByOrderId(String orderId);
}
