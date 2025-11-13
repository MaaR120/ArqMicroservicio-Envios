package com.envios.envios.Entities;

import com.envios.envios.Enum.EstadoShipment;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "shipment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentHistory {

    @Id
    private String id;

    private String shipmentId;      // Vinculación con el envío padre
    private EstadoShipment estado;  // El estado en ese momento
    private LocalDateTime fecha;    // Cuándo ocurrió
    private String operador;        // Opcional: quién hizo el cambio (Sistema, Usuario, etc.)
}