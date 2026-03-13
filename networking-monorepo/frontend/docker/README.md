# Entorno de Desarrollo – Docker & Compose

Este repositorio utiliza **Docker Compose como única fuente de verdad** para el entorno de desarrollo, con **Dockerfiles específicos por servicio** y un **Makefile ligero** para mejorar la ergonomía.

El setup está diseñado intencionadamente para ser:
- Linux-first **a nivel de imágenes base**
- Agnóstico respecto al sistema operativo del desarrollador (Linux, macOS, WSL)
- Independiente del editor
- Enfocado solo a desarrollo (sin optimizaciones prematuras de producción)
- Explícito en ownership, red y estado

Este documento explica **el porqué de cada decisión**, no solo el cómo, para que el entorno pueda mantenerse y evolucionar sin suposiciones implícitas.

---

## Tabla de Contenidos

1. [Principios de Diseño](#principios-de-diseño)
2. [Por qué Docker Compose (archivo único)](#por-qué-docker-compose-archivo-único)
3. [Estructura del Repositorio](#estructura-del-repositorio)
4. [Dockerfiles: Razonamiento y Diseño](#dockerfiles-razonamiento-y-diseño)
   - [Gestión de UID / GID](#gestión-de-uid--gid)
   - [Por qué usamos UID/GID numéricos](#por-qué-usamos-uidgid-numéricos)
   - [Frontend (Astro + pnpm)](#frontend-astro--pnpm)
   - [Backend (Go + hot reload)](#backend-go--hot-reload)
5. [Estrategia del Store de pnpm](#estrategia-del-store-de-pnpm)
6. [Red y Descubrimiento de Servicios](#red-y-descubrimiento-de-servicios)
7. [Por qué sobreescribimos comandos en Compose](#por-qué-sobreescribimos-comandos-en-compose)
8. [Guía de Uso del Makefile](#guía-de-uso-del-makefile)
9. [Fallos Comunes (y por qué están resueltos)](#fallos-comunes-y-por-qué-están-resueltos)
10. [Qué NO hace este setup (a propósito)](#qué-no-hace-este-setup-a-propósito)
11. [Cómo extender este setup](#cómo-extender-este-setup)

---

## Principios de Diseño

El entorno se construye sobre unos principios claros:

- **Compose define el sistema**, no el editor
- **Los Dockerfiles definen runtimes**, no flujos de trabajo
- **Nada corre como root en runtime**
- **Los bind mounts deben comportarse como un filesystem Linux local**
- **El estado es explícito** (volúmenes, puertos, usuarios)
- **Desarrollo ≠ Producción**

El objetivo es que la complejidad sea proporcional a las necesidades reales.

---

## Por qué Docker Compose (archivo único)

Se utiliza **un único `docker-compose.yml`** de forma deliberada:

- Representa la **topología completa del sistema**
- Es ejecutable por cualquiera con Docker
- Encaja conceptualmente con CI y futuros despliegues
- Evita fragmentación en múltiples archivos

No se utilizan:
- Un compose por servicio
- Compose específicos por editor
- Overrides implícitos

Si el sistema crece, se pueden añadir overlays más adelante.  
Por ahora, simplicidad.

---

## Estructura del Repositorio

```text
.
├─ docker-compose.yml
├─ Makefile
├─ docker/
│  ├─ frontend/
│  │   └─ dev.Dockerfile
│  └─ backend/
│      └─ dev.Dockerfile
├─ apps/
│  ├─ frontend/    # Astro
│  └─ backend/     # Go
````

Regla clave:

> El código de aplicación vive en `apps/`
> La infraestructura vive en `docker/` y Compose

---

## Dockerfiles: Razonamiento y Diseño

Cada servicio tiene su **Dockerfile de desarrollo**.
No son imágenes de producción.

### Gestión de UID / GID

Cuando se usan bind mounts (`.:/app`), Docker escribe archivos con el **usuario del contenedor**.

Si ese usuario no coincide con el usuario del host:

* Los archivos quedan como root
* Git deja de funcionar correctamente
* pnpm y herramientas de Go fallan con `EACCES`

**Solución:**
Alinear UID/GID del contenedor con los del host.

Esto se consigue:

1. Calculando `HOST_UID` / `HOST_GID` en el Makefile
2. Pasándolos como build args (`UID`, `GID`)
3. Usando siempre UID/GID **numéricos** en los Dockerfiles

### Por qué usamos UID/GID numéricos

Las imágenes base suelen traer usuarios predefinidos:

* `node`
* usuarios internos de Go o Debian

Por tanto:

* No podemos asumir que un usuario llamado `app` exista
* Los nombres no son estables
* Los números sí

Por eso:

* Se comprueba si el UID/GID existe
* Se reutiliza si existe
* Se hace `chown` usando números
* Se ejecuta el contenedor con `USER ${UID}`

Esto funciona independientemente de la imagen base.

---

## Frontend (Astro + pnpm)

### Por qué pnpm

* Instalaciones deterministas
* Más rápido
* Bien adaptado a monorepos

### Store de pnpm

* Ubicado en `/home/app/.pnpm`
* Respaldado por un **volumen Docker**
* No es un servicio separado

Motivo:

* pnpm espera cercanía al filesystem
* Un “contenedor de pnpm” es una abstracción incorrecta

### HMR

* Astro se ejecuta con `pnpm dev -- --host`
* Se enlaza a `0.0.0.0`
* Permite acceso desde el host independientemente del SO

---

## Backend (Go + hot reload)

### Por qué hot reload

Go no observa archivos por sí mismo.

Dentro de Docker:

* Reiniciar contenedores en cada cambio es lento
* Ejecutar tooling en el host rompe la paridad

### Por qué `air`

`air` proporciona:

* Watch de archivos
* Rebuild automático
* Restart del binario
* Configuración mínima

Es **solo para desarrollo**.

El módulo se referencia como:

```text
github.com/air-verse/air
```

para cumplir con las reglas modernas de módulos de Go.

---

## Estrategia del Store de pnpm

```yaml
volumes:
  pnpm-store:
```

Montado únicamente en el frontend.

Beneficios:

* Reinstalaciones rápidas
* Persistencia entre rebuilds
* Sin ensuciar el host
* Estado explícito

---

## Red y Descubrimiento de Servicios

* Red Docker privada
* Los servicios se comunican por **nombre de servicio**, nunca `localhost`

Ejemplos:

* Frontend → `http://backend:8080`
* Backend → `postgres:5432`

Puertos expuestos solo para desarrollo:

* Frontend: `4321`
* Backend: `8080`
* Postgres: `5432`

---

## Por qué sobreescribimos comandos en Compose

Dockerfile:

* Define un **comportamiento por defecto**

Compose:

* Define **cómo se usa la imagen en este sistema**

Esto permite:

* Reutilizar imágenes
* Cambiar flags sin rebuild
* Evitar proliferación de imágenes

Regla práctica:

> El Dockerfile define defaults
> Compose define intención

---

## Guía de Uso del Makefile

El Makefile es una **capa fina de UX** sobre Docker Compose.

Sirve para:

* Evitar comandos largos
* Pasar UID/GID correctamente
* Facilitar descubrimiento

### Comandos disponibles

```bash
make help        # Lista de comandos
make up          # Arrancar servicios
make down        # Parar servicios
make build       # Construir imágenes
make restart     # Reiniciar servicios
make logs        # Ver logs
make ps          # Estado de contenedores
make shell-fe    # Shell en frontend
make shell-be    # Shell en backend
make db          # Acceso psql
make clean       # Parar y borrar volúmenes
```

### Por qué `HOST_UID` / `HOST_GID`

Algunos shells definen `UID` como variable readonly.
Se evita el conflicto usando nombres explícitos.

---

## Fallos Comunes (y por qué están resueltos)

* Archivos como root → paridad UID/GID
* Errores de pnpm → store explícito
* HMR roto → `--host` + bind mounts
* Fallos al instalar tools Go → paths correctos
* Grupos duplicados → creación condicional

