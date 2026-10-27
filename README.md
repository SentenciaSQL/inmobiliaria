# Alzada

Plataforma SaaS inmobiliaria para constructoras de República Dominicana. Una sola aplicación atiende el marketplace público, el panel de cada constructora y el portal de sus compradores. Los datos de cada empresa viven en el mismo PostgreSQL, aislados por `tenant_id`, por la membresía del usuario autenticado y por Row-Level Security.

## Requisitos

- Docker con Compose, o bien Java 21, Maven y Node.js 22 si ya dispone de PostgreSQL 16.

La vía prevista para el entorno local es Docker Compose. Publica los puertos solo en `127.0.0.1`.

## Arranque con Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

Cuando los servicios estén sanos:

| Servicio | Dirección |
| --- | --- |
| Aplicación | http://127.0.0.1:8088 |
| Correo capturado (Mailpit) | http://127.0.0.1:8025 |
| Consola MinIO | http://127.0.0.1:9001 |
| API OpenAPI | http://127.0.0.1:8088/swagger-ui.html |

MinIO usa el usuario `alzada` y la clave `alzada-local-secret`, salvo que los cambie en `.env`. El perfil `local` carga dos constructoras de demostración. No lo use en producción: ese perfil también inserta usuarios y operaciones de ejemplo.

## Cuentas de demostración

La contraseña de todas es `AlzadaLocal1!`.

| Correo | Acceso |
| --- | --- |
| admin@alzada.do | Administrador de la plataforma |
| maria.gomez@altamar.do | Administradora de Altamar Desarrollos |
| pedro.santos@altamar.do | Asesor comercial de Altamar |
| laura.diaz@altamar.do | Finanzas de Altamar |
| juan.perez@correo.do | Comprador de Altamar (Torre Brisa, unidad A-201) |
| carlos.mejia@horizonte.do | Administrador de Grupo Horizonte |
| ana.rios@correo.do | Asesora de Altamar y de Grupo Horizonte |

Ana debe elegir la constructora activa. El servidor rechaza un `X-Company-Id` que no corresponda a una membresía. El marketplace muestra Torre Brisa y Villas del Norte. Residencial Palma permanece en borrador y no es público. El abono `TRX-ABONO-201` está reportado: la cuota sigue pendiente hasta que finanzas lo confirme.

## Arranque sin Docker

Use esta vía solo si PostgreSQL 16 ya está en marcha con la base `inmobiliaria`, el rol propietario `inmobiliaria` y el rol de aplicación `inmobiliaria_app` (`NOSUPERUSER NOBYPASSRLS`). Flyway debe conectarse con el propietario. La API debe conectarse con `inmobiliaria_app`.

```bash
cd backend
SPRING_PROFILES_ACTIVE=local \
STORAGE_TYPE=local \
STORAGE_LOCAL_DIR=./data/storage \
EMAIL_ENABLED=false \
mvn spring-boot:run
```

En otra terminal:

```bash
cd frontend
npm ci
npm start
```

La interfaz queda en http://localhost:4200 y el proxy envía `/api` a http://localhost:8080. Sin Mailpit, deje `EMAIL_ENABLED=false`: la operación se guarda y el correo queda marcado como no enviado. Con Mailpit en el puerto 1025 puede omitir esa variable.

`STORAGE_TYPE=local` guarda archivos en disco. Con Compose el valor es `s3` y los archivos van a MinIO. Los binarios no se guardan en PostgreSQL.

## Qué hace cada espacio

- Marketplace en español, con precios en DOP y filtros por provincia, municipio, precio y tipo.
- Panel de la constructora: empresa y logo, proyectos, unidades, prospectos, reservas, compradores, contratos, planes de pago, comprobantes y auditoría.
- Portal del comprador, limitado a las operaciones de la constructora seleccionada.
- Administración de la plataforma: constructoras, estado de la cuenta, métricas y plantillas globales.

El logo lo sube el administrador de la constructora (PNG, JPEG o WebP, hasta 2 MB). SVG se rechaza. Si no hay logo, la interfaz muestra un avatar con las iniciales. Las reservas temporales vencen solas y liberan la unidad. Un contrato no se crea al vencer o cancelar una reserva.

## Producción

Defina `SPRING_PROFILES_ACTIVE=prod` para apagar Swagger y el dato de demostración. Antes de publicar:

- `JWT_SECRET` de al menos 32 caracteres, distinto del valor local.
- `COOKIE_SECURE=true` detrás de HTTPS.
- `DATABASE_USER` apuntando al rol sin `BYPASSRLS`. Flyway sigue usando el propietario.
- `STORAGE_TYPE=s3` con un bucket privado. Los documentos se descargan por la API, con autorización.
- `CORS_ORIGINS` y `APP_PUBLIC_URL` con el dominio real.
- No publique el puerto de PostgreSQL ni las credenciales de MinIO.

La API escucha en el puerto 8080. El contenedor web publica el puerto 8080 interno en `127.0.0.1:8088`.
