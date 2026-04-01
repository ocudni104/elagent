FROM node:20-bookworm

# ---- build args for /HOST_UID/GID parity ----
ARG UID=1000
ARG GID=1000

# ---- system setup -----
RUN set -eux; \
  if ! getent group ${GID} >/dev/null; then \
    groupadd -g ${GID} app; \
  fi; \
  if ! getent passwd ${UID} >/dev/null; then \
    useradd -m -u ${UID} -g ${GID} app; \
  fi

# ---- pnpm ----
ENV PNPM_HOME=/home/app/.pnpm
ENV PATH=$PNPM_HOME:$PATH

RUN corepack enable && corepack prepare pnpm@9.0.0 --activate

# ---- working dir ----
WORKDIR /workspace/frontend

# ---- permissions ----
# run as root first
USER root

RUN mkdir -p /home/app/.pnpm /workspace/frontend/node_modules \
 && chown -R ${UID}:${GID} /home/app /workspace/frontend


USER ${UID}

# ---- default ----
CMD ["pnpm", "dev"]
