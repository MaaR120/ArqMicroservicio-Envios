package com.envios.envios.Repository;

import com.envios.envios.Entities.ShipmentHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ShipmentHistoryRepository extends MongoRepository<ShipmentHistory, String> {
    // Buscar todos los estados por ID de envío, ordenados por fecha
    List<ShipmentHistory> findByShipmentIdOrderByFechaAsc(String shipmentId);
}