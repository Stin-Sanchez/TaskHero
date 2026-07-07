# TaskHero — Command Center

Gestor de tareas gamificado con chat en tiempo real. Cada misión completada da XP, sube de nivel, mantiene rachas diarias y desbloquea el chat táctico (privado y por gremios) vía WebSocket.

## Stack

- **Backend:** Java 17, Spring Boot 3.5, Spring Security + JWT, Spring Data JPA, WebSocket (STOMP), MySQL.
- **Frontend:** HTML/CSS/JS vanilla + jQuery, Bootstrap 5, SockJS + StompJS.
- **Arquitectura:** hexagonal (domain / application / infrastructure) — dominio aislado de JPA y de los controladores REST.

```
src/main/java/.../chat_websocket/
├── domain/            # Modelos y reglas de negocio (Usuario, Tarea, GrupoChat, Nivel...)
├── application/        # Casos de uso, servicios, DTOs, puertos (in/out)
└── infrastructure/     # Adaptadores: REST, persistencia JPA, seguridad JWT, WebSocket, email
```

## Funcionalidades

- **Misiones (tareas):** CRUD, prioridad (baja/media/alta), fecha límite, filtros y paginación.
- **Gamificación:** XP por tarea completada, bono por vaciar el tablero del día, login diario y racha, niveles con desbloqueo de funciones (el chat requiere nivel 5).
- **Enlace Social (chat):** mensajería privada y por gremios en tiempo real sobre WebSocket/STOMP, indicador de "escribiendo...", historial vía REST.
- **Gremios:** fundar, invitar y aceptar invitaciones.
- **Notificaciones:** en vivo (toast + campana) y persistidas, con tipo semántico (mensaje, racha, nivel, logro, invitación, sistema).
- **Cuenta:** registro, login, recuperación de contraseña por email, edición de perfil y avatar.

## Requisitos

- Java 17+
- Maven (o el wrapper `mvnw` incluido)
- MySQL 8+

## Configuración

1. Copia la plantilla de configuración:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
2. Completa tus credenciales de MySQL, un `JWT_SECRET` propio y las credenciales SMTP (o expórtalas como variables de entorno en vez de editar el archivo):

   | Variable            | Uso                                            |
   |----------------------|-------------------------------------------------|
   | `DB_URL`             | Cadena de conexión JDBC a MySQL                 |
   | `DB_USERNAME` / `DB_PASSWORD` | Credenciales de la base de datos       |
   | `JWT_SECRET`         | Clave para firmar los tokens (Base64, 256+ bits)|
   | `DB_ENCRYPTION_KEY`  | Clave de cifrado de mensajes en base de datos    |
   | `MAIL_USERNAME` / `MAIL_PASSWORD` | Credenciales SMTP para recuperación de contraseña / avisos por correo |

   `spring.jpa.hibernate.ddl-auto=update` crea el esquema automáticamente al arrancar, no hace falta migrar nada a mano.

## Ejecutar

```bash
./mvnw spring-boot:run
```

La app queda disponible en [http://localhost:8080](http://localhost:8080) (`server.port` configurable). El frontend estático (`src/main/resources/static`) se sirve directamente desde Spring Boot.

## Endpoints principales

| Método | Ruta                                          | Descripción                        |
|--------|-----------------------------------------------|-------------------------------------|
| POST   | `/api/auth/register` / `/login`               | Registro e inicio de sesión         |
| POST   | `/api/auth/forgot-password` / `/reset-password` | Recuperación de contraseña        |
| GET/PUT| `/api/auth/profile`                           | Ver/editar perfil                   |
| GET    | `/api/auth/heroes`                            | Listar héroes (para chat/gremios)   |
| GET/POST/PUT/DELETE | `/api/tareas`                    | CRUD de misiones                    |
| PATCH  | `/api/tareas/{id}/completar`                  | Completar misión (dispara XP)       |
| GET    | `/api/tareas/stats`                           | Estadísticas del tablero            |
| GET    | `/api/chat/privado/{receptorId}`              | Historial de chat privado           |
| GET/POST | `/api/chat/grupos`                          | Listar/crear gremios                |
| POST   | `/api/chat/grupos/{grupoId}/invitar/{receptorId}` | Invitar a un gremio              |
| GET    | `/api/notificaciones`                         | Notificaciones del usuario          |
| PATCH  | `/api/notificaciones/{id}/leer`               | Marcar notificación como leída      |

WebSocket (STOMP sobre SockJS) en `/ws`, con tópicos `/topic/chat/private/{userId}`, `/topic/chat/group/{grupoId}` y `/topic/notifications/{userId}`.

## Seguridad

- Autenticación stateless con JWT (header `Authorization: Bearer <token>`).
- Mensajes de chat cifrados en base de datos (`app.db.encryption-key`).
- No commitees `application.properties` con credenciales reales — usa `application.properties.example` como plantilla y variables de entorno en producción.
