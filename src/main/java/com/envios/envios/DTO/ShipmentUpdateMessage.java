package com.envios.envios.DTO;

import com.envios.envios.Enum.EstadoShipment;
import com.envios.envios.Enum.Transportista;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el mensaje de 'shipment.updateStatus'
 * Esto lo enviarás a tu propio exchange 'shipment_exchange'
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentUpdateMessage {
    private String shipmentId;
    private EstadoShipment estado;
    private Transportista transportista;
}
