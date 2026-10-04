# BACKEND | API GATEWAY - MICROSERVICES - OAUTH 2.0

Proyecto backend que implementa una arquitectura compuesta por API Gateway, 2 microservicios (Pedidos e Inventario) y un servidor de autenticación.

## Características

- API Gateway implementado con **Spring Cloud Gateway** y **Spring Security** y es el único punto público del sistema.
- Implementación de microservicios de Pedidos e Inventario en **Java 25** con **Spring Boot**, **Spring Security**, **Spring Data R2dbc** y **WebFlux**.
- Utiliza **Keycloak** para implementar la autenticación y autorización mediante tokens JWT.
- El API Gateway, los microservicios, las bases de datos y Keycloak están **dockerizados**.

## Funcionalidades

- API Gateway: Enrutador. Único punto público del sistema.
- Microservicio de Pedidos: Administra pedidos, estados e historial. Se comunica con Inventario mediante HTTP reactivo usando WebClient.
- Microservico de Inventario: Administra productos, stock y disponibilidad.

### Requisitos Previos

---

- Java: JDK 25
- Maven: Para construir el proyecto
- Docker: Para ejecutar los contenedores

### Ejecución

---

1. Completar sus credenciales de base de datos dentro del archivo docker-compose.yml
   - `POSTGRES_USER:`
   - `POSTGRES_PASSWORD:`
   - `SPRING_R2DBC_USERNAME:`
   - `SPRING_R2DBC_PASSWORD:`

2. Iniciar las bases de datos, Keycloak, microservicios y api-gateway con Docker Compose

```bash
docker compose up --build
```

3. Crear el reino, cliente y usuario en Keycloak

- Acceder al panel en el navegador:

```bash
http://localhost:8082

```

- Ingresar credenciales:

```bash
Username or email: admin
Password: admin
```

- Crear el reino en **Manage realms** con nombre `orders-inventory-realm`, el cliente en **Clients** y el usuario en **Users** (posteriormente crearle una contraseña en 'Credentials').

4. Completar los campos del archivo _scripts/get-token.sh_ con los datos creados en el panel de Keycloak

- `client_id=`
- `username=`
- `password=`

5. Generar el token mediante el siguiente comando:

```bash
docker compose run --rm token-helper
```

6. Colocar el token en el header Authorization Bearer <token> de las peticiones
7. Realizar las peticiones al **API GATEWAY** mediante cliente postman o de su preferencia

## ENDPOINTS

**Microservicio de Pedidos:**

- `POST /api/v1/orders`: crear un pedido.
- `GET /api/v1/orders`: obtener todos los pedidos.
- `GET /api/v1/{id}/history`: obtener el historial de un pedido.
- `GET /api/v1/orders/{id}`: obtener el estado de un pedido (PENDING, CONFIRMED, CANCELLED y DELIVERED)
- `PATCH /api/v1/orders/{id}/cancel`: cancelar pedidos.

**Microservicio de Inventario:**

- `POST /api/v1/products`: crear un producto.
- `GET /api/v1/products`: obtener todos los productos.
- `GET /api/v1/products/{id}`: buscar producto por id.
- `GET /api/v1/products/{id}/reduce-stock`: reduce el stock del inventario.
- `PUT /api/v1/products/{id}`: actualiza un producto.
- `DELETE /api/v1/products/{id}`: elimina/borra un producto.
