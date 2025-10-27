package com.envios.envios.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el mensaje de 'order_placed'
 * Esto es lo que simularemos (y lo que OrderMS deberia enviar).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedMessage {
    private String orderId;
    private String direccion;
    // Puedes añadir más campos (cartId, articles) si los necesitas, 
    // pero para crear el envío, solo necesitas estos dos.
}