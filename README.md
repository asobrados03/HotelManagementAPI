# HotelManagementAPI - Ejercicio de DeHaroHub
¡Hola y bienvenido!
Este desarrollo se trata de un ejercicio propuesto en la comunidad de Skool DeHaroHub por Nacho De Haro (el creador de la comunidad) en [este repositorio](https://github.com/Deharotech/DHHotel). En el propio enunciado pone que se debe realizar una PR en ese mismo repositorio para que toda la gente de la comunidad pueda revisarlo, aprender de él y aportar su granito de arena. Pero como la comunidad esta inactiva y actualmente cerrada porque el creador esta a otras cosas he optado por publicar mi desarrollo en este repositorio.

## Índice

- [Descripción](#descripción)
- [Objetivo](#objetivo)
- [Características Funcionales](#características-funcionales)
- [Requerimientos Técnicos](#requerimientos-técnicos)
- [Arquitectura del Proyecto](#arquitectura-del-proyecto)
- [Tecnologías Utilizadas](#tecnologías-utilizadas)
- [Instalación y Configuración](#instalación-y-configuración)
  - [Requisitos Previos](#requisitos-previos)
  - [Configuración de Docker](#configuración-de-docker)
  - [Ejecutar la API](#ejecutar-la-api)
- [Documentación de la API](#documentación-de-la-api)
- [Pruebas](#pruebas)
- [Desafíos Adicionales y Mejoras Futuras](#desafíos-adicionales-y-mejoras-futuras)
- [Contribuciones](#contribuciones)
- [Licencia](#licencia)

## Descripción

Esta API RESTful permite la gestión completa de un hotel, incluyendo la administración de clientes, habitaciones, reservas, pagos y administradores. Se ha desarrollado utilizando Java con Spring Boot, aplicando los principios de una arquitectura limpia para separar la lógica de negocio del acceso a datos y la presentación.

## Objetivo

El objetivo de este proyecto es crear una API para la gestión de un hotel que permita:

- Manejar reservas, habitaciones, pagos, clientes y administradores.
- Implementar la lógica de negocio, estructuras de datos y endpoints necesarios.
- Aprender las habilidades en la creación de APIs RESTful, mejorar el manejo de bases de datos y aplicación de buenas prácticas en arquitectura de software.

## Características Funcionales

### Clientes
- **CRUD**: Crear, leer, actualizar y eliminar clientes.
- **Campos obligatorios**: ID, Nombre, Apellidos, Correo electrónico, Número de teléfono.

### Habitaciones
- **CRUD**: Crear, leer, actualizar y eliminar habitaciones.
- **Campos obligatorios**: ID, Número de habitación, Tipo de habitación (simple, doble, suite), Precio por noche, Estado (disponible, ocupada, en mantenimiento).

### Reservas
- **CRUD**: Crear, leer, actualizar y cancelar reservas.
- **Campos obligatorios**: ID, ID del cliente, ID de la habitación, Fecha de inicio, Fecha de fin, Estado (pendiente, confirmada, cancelada).

### Pagos
- **Registro de pagos**: Asociados a una reserva.
- **Campos obligatorios**: ID, ID de la reserva, Monto, Fecha de pago, Método de pago (tarjeta, efectivo, transferencia).

### Administradores
- **CRUD**: Crear, leer, actualizar y eliminar administradores.
- **Campos obligatorios**: ID, Nombre, Correo electrónico, Contraseña (hasheada), Rol (admin, superadmin).

### Autenticación y Autorización
- Implementación de autenticación JWT para administradores y clientes.
- Solo los administradores pueden crear, actualizar o eliminar habitaciones y reservas.
- Los clientes pueden crear y ver sus reservas, pero no modificarlas una vez confirmadas.

## Requerimientos Técnicos

- **Lenguaje y Framework**: Java con Spring Boot.
- **Base de Datos**: MariaDB ejecutado en un contenedor Docker, con un script sql para crear y poblar las tablas de la base de datos.
- **ORM**: No he usado ningún ORM en su lugar he usado JDBC de manera directa ya que es más flexible.
- **Contenedores**: Docker para MariaDB y Adminer.
- **Documentación**: Swagger/OpenAPI para la documentación de la API.
- **Pruebas**: Pruebas unitarias y de integración con JUnit5, Mockito, Testcontainers y Postman para pruebas manuales de la API.

## Diagrama Entidad-Relación de la Base de Datos de la API

```mermaid
erDiagram
    User {
        INT id PK
        VARCHAR email
        VARCHAR password
        ENUM role "CLIENT, ADMIN, SUPERADMIN"
    }

    Client {
        INT id PK
        INT user_id FK
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR phone
    }

    Administrator {
        INT id PK
        INT user_id FK
        VARCHAR name
    }

    Room {
        INT id PK
        INT room_number
        ENUM room_type "SINGLE, DOUBLE, SUITE"
        DECIMAL price_per_night
        ENUM status "AVAILABLE, OCCUPIED, MAINTENANCE"
    }

    Reservation {
        INT id PK
        INT client_id FK
        INT room_id FK
        DECIMAL total_price
        DATE start_date
        DATE end_date
        ENUM status "PENDING, CONFIRMED, CANCELED"
    }

    Payment {
        INT id PK
        INT reservation_id FK
        DECIMAL amount
        DATE payment_date
        ENUM payment_method "CARD, CASH, TRANSFER"
    }

    Client ||--o{ Reservation : has
    Room ||--o{ Reservation : "is booked in"
    Reservation ||--o{ Payment : has
    User ||--|| Client : "is a"
    User ||--|| Administrator : "is a"
```

## Arquitectura del Proyecto

El proyecto está organizado siguiendo una arquitectura limpia:

- **Adaptadores**:  
  - Controladores REST para recibir peticiones y enviar respuestas.
  - DTOs para el mapeo de datos entre la API y la lógica de negocio.
  - Adaptador de seguridad (JWT, filtros, etc.).
  
- **Aplicación**:  
  - Casos de Uso que orquestan la lógica de negocio.

- **Dominio**:  
  - Entidades de dominio (Modelos) y lógica de negocio.
  - Interfaces de repositorios (Puertos).

- **Infraestructura**:  
  - Implementaciones de repositorios (con JDBC).
  - Configuración de acceso a la base de datos (MariaDB en Docker).
 
### Diagrama hexagonal de la arquitectura general y limpia de la API

![Arquitectura hexagonal de la API](resources/Arquitectura-hexagonal-API-Gestión-Hotel.png)

### Diagrama de flujo general de la arquitectura

```mermaid
graph TD;
  subgraph CLIENTE
    Client["💻 Cliente (Front-end)"]
  end

  subgraph ADAPTADORES
    Controller["🌐 @RestController Controladores Web"]
    DTOs["🔌 DTOs 
    (Request/Response)"]
    Security["🔐 Filtros y JWT 
    (Adaptador de Seguridad)"]
  end

  subgraph APLICACIÓN
    UseCase["⚙️ Casos de Uso 
    (Lógica de Negocio)"]
  end

  subgraph DOMINIO
    Entities["🗃️ Entidades de Dominio (Modelos)"]
    Ports["📁 Interfaces de Repositorios (Puertos)"]
  end

  subgraph INFRAESTRUCTURA
    RepoImpl["📂 @Repository Implementación de Repositorios con JDBC"]
    DB["🗄️🐋 Base de Datos (MariaDB)"]
  end

  Client -- "HTTP Request" --> Controller
  Controller -- "DTO Mapping" --> UseCase
  UseCase -- "Invoca reglas de negocio" --> Entities
  UseCase -- "Solicita persistencia" --> Ports
  Ports -- "Implementado por" --> RepoImpl
  RepoImpl -- "Acceso a datos" --> DB
  Controller -- "HTTP Response" --> Client
  %% Opcional: Integración de seguridad
  Controller -- "Autenticación/Autorización" --> Security
```

### Esquema del Proceso de Autenticación/Registro basado en JWT (JSON Web Tokens)

```mermaid
graph TD;
    subgraph CLIENTE
        Cliente["💻 Cliente (Front-end)"]
    end

    subgraph FILTROS
        JwtFilter["🔍 JwtAuthenticationFilter{}🔸 Verifica si el JWT es null"]
    end

    subgraph AUTENTICACION
        AuthController["🔐 AuthenticationController{}"]
        AuthService["⚙️ AuthenticationService{}"]
        JwtService["🛠️ JwtService{}
        🔸Genera JWT Token"]
    end

    subgraph REPOSITORIO
        UserRepo["📂UserRepository{} 🔸Guarda/Obtiene UserDetail"]
        User["🧑‍💼 User{}
        🔸Implementa UserDetails"]
    end

    subgraph CONFIGURACION
        Config["⚙️ ApplicationConfig 🔸Authentication Manager 🔸Providers 🔸PasswordEncoders"]
    end

    subgraph BASE DE DATOS
        DB[("🗄️🐋 Base de Datos (MariaDB)")]
    end

    %% Flujo del proceso de autenticación
    Cliente -- "HTTP Request" --> JwtFilter
    JwtFilter --> AuthController
    AuthController --> AuthService
    AuthService --> UserRepo
    UserRepo --> User
    User --> DB

    AuthService --> JwtService
    JwtService --> AuthController
    AuthController -- "HTTP Response (JWT Token)" --> Cliente

    %% Conexiones de configuración
    Config -.-> AuthService
```

### Esquema del Proceso de Validación JWT

```mermaid
graph TD;
    subgraph CLIENTE
        Cliente["💻 Cliente (Front-end)"]
    end

    subgraph FILTROS
        JwtFilter["🔍 JwtAuthenticationFilter{}🔸 Verifica el JWT"]
    end

    subgraph SERVICIOS
        JwtService["🛠️ JwtService{}
        🔸Extrae el usuario del JWT 
        🔸Verifica el token"]
        UserDetailsService["⚙️ UserDetailsService{} 🔸 loadUserByUsername()"]
    end

    subgraph REPOSITORIO
        User["🧑‍💼 User{}
        🔸Implementa UserDetails"]
        DB[("🗄️🐋 Base de Datos")]
    end

    subgraph SecurityContext
        Authentication["🔐Authentication
        Principle  | Credentials | Authorities"]
    end

    subgraph CONTROLADOR
        Controller["⚡ Controller{}"]
    end

    %% Flujo del proceso de validación JWT
    Cliente -- "HTTP Request (Token)" --> JwtFilter
    JwtFilter --> JwtService
    JwtService --> UserDetailsService
    UserDetailsService --> User
    User --> DB

    JwtFilter --> Authentication
    Authentication --> Controller
    Controller -- "✅ HTTP Response (JSON)" --> Cliente

    %% Manejo de errores (403)
    JwtFilter -- "❌ HTTP 403: Token inválido" --> Cliente
    JwtFilter -- "❌ HTTP 403: Falta token o usuario no existe" --> Cliente
```

### 🚀 Evolución de Arquitectura: Escalabilidad y Mensajería
Para transformar este monolito en un sistema preparado para alta carga, se realizaron dos mejoras estructurales:
1. **Caché con Redis:** Optimización del endpoint de consulta de habitaciones disponibles mediante `@Cacheable("room-availability")`. La caché tiene un TTL de 5 minutos y se invalida automáticamente al crear, modificar o cancelar una reserva, y también al cambiar el estado de una habitación.
2. **Arquitectura Orientada a Eventos (EDA):** Se desacopló el flujo de notificaciones mediante **RabbitMQ**. Al crear una reserva, la API publica un `BookingCreatedEvent` en el exchange `hotel.exchange` con la routing key `booking.created`. El microservicio independiente `notification-worker` consume la cola `hotel.notifications` y envía un email de confirmación al huésped.

#### Flujo de eventos de reserva

```text
Cliente / Admin
     |
     v
hotel-api
     |
     | BookingCreatedEvent
     v
RabbitMQ (hotel.exchange)
     |
     v
Cola hotel.notifications
     |
     v
notification-worker
     |
     v
Email de confirmación al huésped
```

El sistema de mensajería incluye reintentos automáticos con backoff exponencial usando colas intermedias:

```text
hotel.notifications
     |
     | fallo
     v
retry.1 (5s) -> retry.2 (25s) -> retry.3 (125s) -> hotel.notifications.dlq
```

Si el envío del email falla en los reintentos configurados, el mensaje termina en la Dead Letter Queue `hotel.notifications.dlq` para su revisión posterior.

#### Microservicio `notification-worker`

El proyecto incluye el módulo independiente `notification-worker`, encargado de procesar las notificaciones de reservas:

- Escucha la cola `hotel.notifications`.
- Consume eventos `BookingCreatedEvent` serializados en JSON.
- Envía emails HTML con Spring Mail.
- Usa una plantilla Thymeleaf en español (`booking-confirmation.html`) para el email de confirmación de reserva.

## Tecnologías Utilizadas

- **Java 21+**
- **Spring Boot**
- **Spring Security con JWT**
- **JDBC**
- **RabbitMQ**
- **Redis**
- **Spring AMQP**
- **Spring Cache**
- **Spring Mail**
- **Thymeleaf**
- **Docker**  
  - **MariaDB**
  - **Adminer**
  - **Redis**
  - **RabbitMQ**
- **Swagger/OpenAPI**
- **Gradle**
- **JUnit5**
- **Mockito**
- **Testcontainers**
- **Postman**

## Instalación y Configuración

### Requisitos Previos

- JDK 21 o superior instalado.
- Docker y Docker Compose instalados.
- Git instalado.

### Configuración de Docker

El proyecto incluye un archivo `docker-compose.yml` para levantar la infraestructura necesaria de la aplicación:

- **MariaDB**: base de datos principal, disponible en el puerto `3306`.
- **Adminer**: panel web para gestionar la base de datos, disponible en `http://localhost:8081`.
- **Redis**: caché de disponibilidad de habitaciones, disponible en el puerto `6379`.
- **RabbitMQ**: broker de eventos, disponible por AMQP en el puerto `5672`.
- **Panel de RabbitMQ**: consola de administración disponible en `http://localhost:15672`.

En el despliegue completo, los servicios de la aplicación son:

- **hotel-api**: API principal de gestión del hotel.
- **notification-worker**: microservicio que consume `hotel.notifications` y envía emails de confirmación.
- **mariadb**
- **adminer**
- **redis**
- **rabbitmq**

Para iniciar los contenedores, ejecuta en la raíz del proyecto:

```bash
docker compose up -d
```

### Ejecutar la API

> [!CAUTION]
> Antes de ejecutar el código fuente de la API, el contenedor Docker con la base de datos MariaDB debe estar corriendo.

1. Clona el repositorio:
   ```bash
   git clone https://github.com/asobrados03/HotelManagementAPI.git
   cd HotelManagementAPI
   ```

2. Compila y ejecuta la aplicación:
   ```bash
   ./gradlew build
   ./gradlew bootRun
   ```

3. La API estará disponible en `http://localhost:8080`.

4. Para ejecutar el worker de notificaciones:
   ```bash
   ./gradlew :notification-worker:bootRun
   ```

## Documentación de la API

La documentación interactiva se genera automáticamente con Swagger. Una vez iniciada la aplicación, puedes acceder a ella en:
- `http://localhost:8080/swagger-ui.html` o
- `http://localhost:8080/swagger-ui/index.html`

## Pruebas

Se han implementado pruebas unitarias y de integración para asegurar el correcto funcionamiento de la API. Para ejecutarlas:

```bash
./gradlew test
```

> [!IMPORTANT]
> Para las pruebas de integración debe estar corriendo Docker en la máquina.

## Desafíos Adicionales y Mejoras Futuras

- **Optimización de Consultas:** Mejorar el rendimiento en operaciones complejas sobre reservas y habitaciones.
- **Gestión de Estados:** Refinar la lógica de transición de estados en reservas.
- **Manejo de Concurrencia:** Evitar sobre-reservas mediante bloqueos o estrategias de concurrencia.
- **Escalabilidad:** Adaptar la API para despliegues en entornos distribuidos o en la nube.
- **Seguridad:** Mejorar la protección contra ataques (SQL Injection, XSS, etc.) y optimizar el manejo de autenticación y autorización.

## Contribuciones

¡Las contribuciones son bienvenidas! Si deseas colaborar en el proyecto, sigue estos pasos:

1. Realiza un fork del repositorio.
2. Crea una rama para tu funcionalidad: `git checkout -b feature/nueva-funcionalidad`.
3. Realiza tus cambios y haz commit.
4. Envía un pull request describiendo los cambios realizados.

## Licencia

Este proyecto se distribuye bajo la [Licencia MIT](LICENSE).
