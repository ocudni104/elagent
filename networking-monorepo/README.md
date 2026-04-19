# networking-monorepo

This repository is a monorepo with:

- a standalone `frontend/`
- infra and local runtime wiring under `docker/` and `infra/`
- backend services under `services/`, where each service is its own single Gradle project

## Layout

```text
.
├─ docker/
├─ frontend/
├─ gradle/
├─ infra/
├─ services/
│  ├─ gateway/
│  ├─ identity-provider/
│  ├─ app-service/
│  ├─ channel-service/
│  ├─ insights-service/
│  ├─ workers/
│  └─ ai-layer/
├─ gradle.properties
└─ gradlew
```

## Identity Provider Session Slice

The identity-provider now includes a feature-oriented session slice under `services/identity-provider/src/main/java/ocudni104/idp/session`:

```text
session/
├─ application/
├─ domain/
├─ persistence/
└─ web/
```

Intent by folder:

- `domain`: session entity, typed ID value objects, repository contract, domain exceptions
- `application`: use cases and commands
- `persistence`: JDBC repository implementation
- `web`: request/response DTOs, controller, HTTP exception mapping

## How It Runs

- `frontend/` is not part of a Gradle multi-project build
- each backend service under `services/` has its own `settings.gradle.kts`
- Docker Compose starts each backend by changing into that service directory and running `../../gradlew bootRun`
- service discovery is provided by Consul in local development
- `workers/` is currently not started as a long-running Spring service in Compose

## Prerequisites

- Docker Engine
- Docker Compose v2

Optional but useful:

- Java 21 if you want to run services directly outside Docker
- Node.js and pnpm if you want to run the frontend directly outside Docker

## Run Everything

From the repo root:

```bash
docker compose -f docker/compose/dev.yml --profile dev up -d --build
```

To stop everything:

```bash
docker compose -f docker/compose/dev.yml --profile dev down
```

## Run Individual Services

Examples:

```bash
docker compose -f docker/compose/dev.yml up -d consul nm-postgres
docker compose -f docker/compose/dev.yml up -d gateway
docker compose -f docker/compose/dev.yml up -d frontend
```

You can also enable a specific profile:

```bash
docker compose -f docker/compose/dev.yml --profile gateway up -d
docker compose -f docker/compose/dev.yml --profile frontend up -d
```

## Run A Service Without Docker

Each service is a standalone Gradle project.

Example:

```bash
cd services/gateway
../../gradlew bootRun
```

Another example:

```bash
cd services/channel-service
../../gradlew bootRun
```

Identity provider also has its own local wrapper:

```bash
cd services/identity-provider
./gradlew bootRun
```

## Local Endpoints

- frontend: `http://localhost:4321`
- identity-provider: `http://localhost:8081`
- app-service: `http://localhost:8082`
- channel-service: `http://localhost:8083`
- insights-service: `http://localhost:8084`
- ai-layer: `http://localhost:8085`
- gateway: `http://localhost:8086`
- Consul UI: `http://localhost:8500`
- pgAdmin: `http://localhost:5050`
- Mongo Express: `http://localhost:8087`

## Notes

- backend service discovery uses Consul
- `gateway` resolves `identity-provider` through discovery-aware Spring Cloud wiring
- `nm-postgres` is the local Postgres service name in Docker
- root Gradle is no longer a multi-project backend build; service ownership is local to each service directory
- local Codex plugin `caveman` is present in `plugins/caveman`

## Devcontainer

This repo includes a single repo-level devcontainer for the whole monorepo:

- [`.devcontainer/devcontainer.json`](/home/wmonkey/Work/d-esc-ctrl/networking-monorepo/.devcontainer/devcontainer.json)
- [`.devcontainer/Dockerfile`](/home/wmonkey/Work/d-esc-ctrl/networking-monorepo/.devcontainer/Dockerfile)

It is a tooling container, not an app runtime container. Use it as a consistent workstation for Gradle, Atlas, `psql`, frontend commands, and general repo work while runtime services continue to run through Docker Compose.

Included tools:

- JDK 21
- shared Gradle wrapper usage from the repo
- git, curl, unzip, bash
- Atlas CLI
- PostgreSQL client tools
- Node.js 22 and pnpm via Corepack
- Docker CLI

Basic usage inside the devcontainer:

```bash
cd services/gateway
../../gradlew tasks
```

```bash
cd persistence/atlas
atlas migrate diff init --env local
```

```bash
cd frontend
pnpm install
pnpm dev
```

Windows caveats:

- this setup assumes standard VS Code Dev Containers usage with Docker Desktop
- if you are on Windows, opening the repo through WSL2-backed Docker Desktop is the most reliable path
- Docker access from inside the devcontainer assumes the host Docker socket is available at `/var/run/docker.sock`
- if socket mounting is unavailable in your environment, the devcontainer still works as a tooling container, but Docker commands from inside it will not

## Recommended Tools

### lazydocker

Useful for watching containers, logs, images, and Compose state interactively.

Install on Debian/Ubuntu:

```bash
curl -Lo lazydocker.tar.gz https://github.com/jesseduffield/lazydocker/releases/latest/download/lazydocker_$(uname -s)_$(uname -m).tar.gz
tar xf lazydocker.tar.gz lazydocker
sudo install lazydocker /usr/local/bin/lazydocker
rm -f lazydocker lazydocker.tar.gz
```

Install on Windows with Scoop:

```powershell
scoop install lazydocker
```

Install on Windows with Chocolatey:

```powershell
choco install lazydocker
```

### dive

Useful for inspecting image layers, wasted space, and Dockerfile effects.

Install on Debian/Ubuntu:

```bash
curl -fsSL https://github.com/wagoodman/dive/releases/latest/download/dive_$(uname -s)_$(dpkg --print-architecture).deb -o dive.deb
sudo apt install -y ./dive.deb
rm -f dive.deb
```

Install on Windows with Scoop:

```powershell
scoop install dive
```

Install on Windows with Chocolatey:

```powershell
choco install dive
```
