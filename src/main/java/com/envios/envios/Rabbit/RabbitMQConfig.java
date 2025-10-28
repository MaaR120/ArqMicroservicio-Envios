package com.envios.envios.Rabbit;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // --- Exchange para flujo de órdenes para crear shipment ---
    public static final String ORDER_EXCHANGE = "order_placed";
    public static final String ORDER_PAID_QUEUE = "order_paid_queue";
    public static final String ORDER_PAID_ROUTING_KEY = "order_placed";

    // Cola para actualizar estado del envío
    public static final String SHIPMENT_EXCHANGE = "shipment_exchange"; // Exchange propio
    public static final String SHIPMENT_STATUS_UPDATE_QUEUE = "shipment_status_update_queue";
    public static final String SHIPMENT_STATUS_UPDATE_ROUTING_KEY = "shipment.updateStatus";
    
    // Exchange tipo Topic ORDEN A SHIPMENT
    @Bean
    public FanoutExchange orderExchange() {
        return new FanoutExchange(ORDER_EXCHANGE, false, false);
    }

    // --- Declaración de cola ORDEN A SHIPMENT---
    @Bean
    public Queue orderPaidQueue() {
        return new Queue(ORDER_PAID_QUEUE, false);
    }


    // --- Bindings ORDEN A SHIPMENT---
    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, FanoutExchange orderExchange) {
        return BindingBuilder.bind(orderPaidQueue)
                .to(orderExchange);
    }

    // Exchange tipo Topic SHIPMENT ACTUALIZAR ESTADO

    @Bean
    public DirectExchange shipmentExchange() {
        return new DirectExchange(SHIPMENT_EXCHANGE); 
    }

    // --- Declaración de cola SHIPMENT ACTUALIZAR ESTADO---

    @Bean
    public Queue shipmentStatusUpdateQueue() {
        return new Queue(SHIPMENT_STATUS_UPDATE_QUEUE, true);
    }

    // --- Bindings SHIPMENT ACTUALIZAR ESTADO--
    @Bean
    public Binding shipmentStatusUpdateBinding(Queue shipmentStatusUpdateQueue, 
                                            DirectExchange shipmentExchange) { // <-- CAMBIO
        return BindingBuilder.bind(shipmentStatusUpdateQueue)
                .to(shipmentExchange) 
                .with(SHIPMENT_STATUS_UPDATE_ROUTING_KEY);
}

    // --- Conversor JSON ---
    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        converter.setClassMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
