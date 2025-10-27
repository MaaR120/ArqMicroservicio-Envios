package com.envios.envios.Entities;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.envios.envios.Enum.EstadoShipment;
import com.envios.envios.Enum.Transportista;

@Document(collection = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    private String id;

    private String orderId;
    private String direccion;
    private Transportista transportista;
    private EstadoShipment estado;
    private String trackingCode;
    private Double costo;
}