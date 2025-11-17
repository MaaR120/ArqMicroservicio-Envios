# Acerca de ShipmentMS

Microservicio presentado como caso de estudio para el e-commerce de la cátedra de Arquitectura de microservicios.

Se encarga de gestionar el ciclo de vida de los **envíos (shipments)** de los pedidos del e-commerce.

Su responsabilidad principal es crear un nuevo envío automáticamente cuando una orden es marcada como pagada. Además, permite consultar el estado de los envíos, visualizar su historial de cambios y actualizarlos (cambiando su estado o transportista) tanto por una API REST como por mensajería asíncrona.

---

## 🗄️ MongoDB

La base de datos del microservicio es almacenada en MongoDB.

### 🧩 Estructura de Datos

#### **Shipment**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | String | ID de MongoDB |
| `orderId` | String | ID de la orden |
| `direccion` | String | Dirección de entrega |
| `transportista` | String | Enum `Transportista` |
| `estado` | String | Enum `EstadoShipment` |
| `trackingCode` | String | UUID generado automáticamente |
| `costo` | Double | Costo del envío |

#### **ShipmentHistory**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | String | ID de MongoDB |
| `shipmentId` | String | ID del envío |
| `estado` | String | Estado registrado |
| `fecha` | LocalDateTime | Fecha del cambio |
| `operador` | String | Origen del cambio |


---

## 🐇 Conexiones a otros Microservicios - RabbitMQ

Este microservicio se comunica con los demás del ecosistema del e-commerce a través de RabbitMQ.

### Consumidor de `order_placed`
`ShipmentMS` escucha el exchange `order_placed` (tipo `fanout`) que es publicado por el microservicio de Órdenes. Cuando recibe un mensaje, crea un nuevo `Shipment` en la base de datos con estado `En_Preparacion` y genera el primer registro en el historial.

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
    4.  Se crea un nuevo `Shipment`, se guarda en MongoDB y se registra el evento en `ShipmentHistory`.
  
Este mensaje simula el evento emitido por el microservicio de Órdenes cuando una compra es pagada.

* **Exchange**: `order_placed`
* **Type**: `fanout`
* **Routing Key**: *(Vacía)*
* **Payload (JSON)**:
```json
{
  "__TypeId__": "com.envios.envios.DTO.OrderPlacedMessage",
  "orderId": "ORD-EJEMPLO-001",
  "direccion": "Calle Falsa 123, Springfield"
}
```

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
* **Resultado:** El `Shipment` se actualiza en la base de datos y se genera un registro histórico si hubo cambio de estado.
* **Camino Normal:**
    1.  El usuario envía una solicitud `PATCH /shipments/{id}` con el DTO `ShipmentUpdateDTO`.
    2.  El sistema verifica que el `Shipment` existe.
    3.  El sistema actualiza los campos no nulos del DTO en la entidad.
    4.  El sistema guarda los cambios en MongoDB, registra el cambio en `ShipmentHistory` y devuelve el `Shipment` actualizado.
* **Camino Alternativo:**
    * Si el `shipmentId` no existe, el sistema devuelve un `404 Not Found`.

### 5. CU: Actualizar Envío (vía RabbitMQ)
**Descripción:** Un servicio externo puede solicitar una actualización de un envío de forma asíncrona publicando un mensaje en el *exchange* de `ShipmentMS`.

* **Precondición:** El `Shipment` debe existir.
* **Entradas:** Mensaje de RabbitMQ (DTO: `ShipmentUpdateMessage`) con `shipmentId`, `estado` (opcional) y `transportista` (opcional).
* **Resultado:** El `Shipment` se actualiza en la base de datos.
* **Camino Normal:**
    1.  El consumidor `ShipmentConsumer` escucha un mensaje en la cola `shipment_status_update_queue`.
    2.  El sistema verifica que el `Shipment` (por `shipmentId`) existe.
    3.  El sistema actualiza los campos no nulos del DTO.
    4.  El sistema guarda los cambios en MongoDB y registra el cambio en `ShipmentHistory`.
* **Camino Alternativo:**
    * Si el `shipmentId` no existe, el servicio lanza una `RuntimeException` (manejada por el consumidor para evitar *poison messages*).

Este mensaje simula el evento emitido por el microservicio de Órdenes cuando una compra es pagada.

* **Exchange**: `shipment_exchange`
* **Type**: `direct`
* **Routing Key**: `shipment.updateStatus`
* **Payload (JSON)**:

```json
{
  "__TypeId__": "com.envios.envios.DTO.ShipmentUpdateMessage",
  "shipmentId": "654321abcdef1234567890ab",
  "estado": "EN_CAMINO",
  "transportista": "ANDREANI"
}
```

### 6. CU: Consultar Historial de Estados
**Descripción:** Permite consultar la traza histórica de cambios de estado de un envío.

* **Entradas:** `shipmentId` (String).
* **Salida:** Lista de objetos `ShipmentHistory` ordenados por fecha.
* **Camino Normal:**
    1.  El usuario envía una solicitud `GET /shipments/{id}/history`.
    2.  El sistema verifica que el envío exista.
    3.  El sistema consulta el `ShipmentHistoryRepository` filtrando por `shipmentId`.
    4.  Se devuelve la lista de cambios.
* **Camino Alternativo:**
    * Si el `shipmentId` no existe, se devuelve un `404 Not Found`.

---

## 🌐 Interfaz REST

### Consultar Envío por ID
`GET /shipments/{id}`

* **Path Parameters**
    * `id` (String): ID del envío (MongoDB ID).

* **Response `200 OK`**
    ```json
    {
      "id": "690008e0f892cfa5ed6114a1",
      "orderId": "ORD-1006",
      "direccion": "Avenida Siempre Viva 742",
      "transportista": "CORREO_ARGENTINO",
      "estado": "ENVIADO",
      "trackingCode": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",
      "costo": 1500.0
    }
    ```
* **Otras responses**
    * `404 Not Found` si el ID no existe.

### Consultar Envío por ID de Orden
`GET /shipments/order/{orderId}`

* **Path Parameters**
    * `orderId` (String): ID de la orden asociada.

* **Response `200 OK`**
    (Idéntica a la respuesta anterior)

* **Otras responses**
    * `404 Not Found` si el `orderId` no está asociado a ningún envío.

### Consultar Historial de Estados
`GET /shipments/{id}/history`

* **Path Parameters**
    * `id` (String): ID del envío.

* **Response `200 OK`**
    ```json
    [
        {
            "id": "hist1",
            "shipmentId": "690008e0...",
            "estado": "En_Preparacion",
            "fecha": "2025-11-13T10:00:00",
            "operador": "Creación automática por Orden"
        },
        {
            "id": "hist2",
            "shipmentId": "690008e0...",
            "estado": "ENVIADO",
            "fecha": "2025-11-13T12:30:00",
            "operador": "Actualización de estado"
        }
    ]
    ```

### Actualizar Envío (Parcial)
`PATCH /shipments/{id}`

* **Path Parameters**
    * `id` (String): ID del envío (MongoDB ID) a actualizar.

* **Body (DTO: `ShipmentUpdateDTO`)**
    ```json
    {
      "estado": "ENTREGADO",
      "transportista": null
    }
    ```
    *O (solo transportista):*
    ```json

    {
      "transportista": "ANDREANI"
    }
    ```

* **Response**
    * `200 OK`: Devuelve el objeto `Shipment` completo y actualizado.
    * `404 Not Found`: Si el `id` del envío no existe.
 

