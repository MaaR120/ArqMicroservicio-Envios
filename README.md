# Acerca de ShipmentMS

Microservicio presentado como caso de estudio para el e-commerce de la cátedra de Arquitectura de microservicios.

Se encarga de gestionar el ciclo de vida de los **envíos (shipments)** de los pedidos del e-commerce.

Su responsabilidad principal es crear un nuevo envío automáticamente cuando una orden es marcada como pagada. Además, permite consultar el estado de los envíos y actualizarlos (cambiando su estado o transportista) tanto por una API REST como por mensajería asíncrona.

---

## 🗄️ MongoDB

La base de datos del microservicio es almacenada en MongoDB.

### Estructura de Datos

#### Shipment
* **id**: String (ID de MongoDB)
* **orderId**: String (ID de la orden del microservicio de Órdenes)
* **direccion**: String
* **transportista**: String (Enum: `Transportista`)
* **estado**: String (Enum: `EstadoShipment`)
* **trackingCode**: String (UUID)
* **costo**: Double

---

## 🐇 Conexiones a otros Microservicios - RabbitMQ

Este microservicio se comunica con los demás del ecosistema del e-commerce a través de RabbitMQ.

### Consumidor de `order_placed`
`ShipmentMS` escucha el exchange `order_placed` (tipo `fanout`) que es publicado por el microservicio de Órdenes. Cuando recibe un mensaje, crea un nuevo `Shipment` en la base de datos con estado `En_Preparacion`.

### Consumidor de `shipment_exchange`
`ShipmentMS` expone su propio exchange `shipment_exchange` (tipo `direct`) con una cola `shipment_status_update_queue`. Esto permite que otros servicios (o el *testing* manual) envíen comandos de actualización de forma asíncrona para cambiar el `estado` o `transportista` de un envío.

---

## 📋 Casos de Uso de ShipmentMS

### 1. CU: Crear Envío automáticamente
**Descripción:** Cuando una orden es pagada, el microservicio de Órdenes emite un evento. `ShipmentMS` escucha este evento y crea un registro de envío.

* **Precondición:** El evento `order_placed` ha sido emitido y contiene `orderId` y `direccion`.
* **Entradas:** Mensaje de RabbitMQ con `orderId` y `direccion` (DTO: `OrderPlacedMessage`).
* **Salida:** El `Shipment` se almacena en la base de datos con un `trackingCode` aleatorio y un `estado` inicial de `En_Preparacion`.
* **Camino Normal:**
    1.  El sistema (consumidor `ShipmentConsumer`) escucha el evento `order_placed` en la cola `order_paid_queue`.
    2.  Se extraen `orderId` y `direccion` del mensaje.
    3.  Se verifica (por idempotencia) que no exista ya un envío para esa `orderId`.
    4.  Se crea un nuevo `Shipment` y se guarda en MongoDB.

### 2. CU: Consultar Envío por ID
**Descripción:** Permite a un cliente (otro servicio o un *frontend*) obtener los detalles de un envío específico usando su ID de base de datos.

* **Entradas:** `shipmentId` (String).
* **Salida:** Objeto `Shipment` completo.
* **Camino Normal:**
    1.  El usuario envía una solicitud `GET /shipments/{id}`.
    2.  El microservicio consulta el `ShipmentRepository` por el `id`.
    3.  Se devuelve el `Shipment`.
* **Camino Alternativo:**
    * Si el `id` no existe, se devuelve un `404 Not Found`.

### 3. CU: Consultar Envío por ID de Orden
**Descripción:** Permite a un cliente consultar un envío usando el ID de la orden (`orderId`) a la que pertenece.

* **Entradas:** `orderId` (String).
* **Salida:** Objeto `Shipment` completo.
* **Camino Normal:**
    1.  El usuario envía una solicitud `GET /shipments/order/{orderId}`.
    2.  El microservicio consulta el `ShipmentRepository` usando el método `findByOrderId(orderId)`.
    3.  Se devuelve el `Shipment`.
* **Camino Alternativo:**
    * Si el `orderId` no existe, se devuelve un `404 Not Found`.

### 4. CU: Actualizar Envío (vía REST)
**Descripción:** Un cliente puede actualizar parcialmente un envío (su `estado` y/o `transportista`) usando la API REST.

* **Precondición:** El `Shipment` debe existir.
* **Entradas:** `shipmentId` (String), Body (JSON) con `estado` (opcional) y `transportista` (opcional).
* **Resultado:** El `Shipment` se actualiza en la base de datos.
* **Camino Normal:**
    1.  El usuario envía una solicitud `PATCH /shipments/{id}` con el DTO `ShipmentUpdateDTO`.
    2.  El sistema verifica que el `Shipment` existe.
    3.  El sistema actualiza los campos no nulos del DTO en la entidad.
    4.  El sistema guarda los cambios en MongoDB y devuelve el `Shipment` actualizado.
* **Camino Alternativo:**
    * Si el `shipmentId` no existe, el sistema devuelve un `404 Not Found`.

### 5. CU: Actualizar Envío (vía RabbitMQ)
**Descripción:** Un servicio externo puede solicitar
