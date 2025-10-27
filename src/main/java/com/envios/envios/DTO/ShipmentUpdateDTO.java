package com.envios.envios.DTO;

import com.envios.envios.Enum.EstadoShipment;
import com.envios.envios.Enum.Transportista;
import lombok.Data;

@Data
public class ShipmentUpdateDTO {
    // El cliente puede enviar uno, el otro, o ambos.
    // Jackson los mapeará a null si no vienen en el JSON.
    private EstadoShipment estado;
    private Transportista transportista;
}