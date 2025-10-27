package com.envios.envios.Rabbit;

import com.envios.envios.Service.ShipmentService;
import com.envios.envios.DTO.OrderPlacedMessage;
import com.envios.envios.DTO.ShipmentUpdateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ShipmentConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentConsumer.class);
    private final ShipmentService shipmentService;

    public ShipmentConsumer(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * Listener para la creación de envíos.
     * Escucha la cola 'order_paid_queue'
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    public void handleOrderPlaced(OrderPlacedMessage message) {
        log.info("Mensaje 'Order Placed' recibido para orderId: {}", message.getOrderId());

        try {
            // El servicio ya es idempotente, así que solo llamamos a crear
            shipmentService.createShipment(message.getOrderId(), message.getDireccion());
            log.info("Shipment creado o ya existente para orderId: {}", message.getOrderId());
            
        } catch (Exception e) {
            // ¡IMPORTANTE! Capturamos la excepción para que el mensaje no
            // entre en un bucle de reintentos (poison message).
            log.error("Error al procesar 'Order Placed' para orderId: {}. Error: {}", 
                         message.getOrderId(), e.getMessage());
        }
    }

    /**
     * Listener para la actualización de envíos.
     * Escucha la cola 'shipment_status_update_queue'
     */
    @RabbitListener(queues = RabbitMQConfig.SHIPMENT_STATUS_UPDATE_QUEUE)
    public void handleShipmentUpdate(ShipmentUpdateMessage message) {
        log.info("Mensaje 'Shipment Update' recibido para shipmentId: {}", message.getShipmentId());

        try {
            // El servicio maneja la lógica de actualización y lanza error si no existe
            shipmentService.updateShipment(
                message.getShipmentId(),
                message.getEstado(),
                message.getTransportista()
            );
            log.info("Shipment actualizado: {}", message.getShipmentId());

        } catch (RuntimeException e) { 
            // Capturamos RuntimeException (ej. "Shipment not found")
            log.error("Error al actualizar shipment {}: {}. El mensaje será descartado.", 
                         message.getShipmentId(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al actualizar shipment {}: {}. El mensaje será descartado.", 
                         message.getShipmentId(), e.getMessage(), e);
        }
    }
}