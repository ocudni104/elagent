# docker/backend/frontend.dev.Dockerfile
FROM golang:1.25-bookworm

ARG UID=1000
ARG GID=1000

# ---- user ----
# ---- system setup -----
RUN set -eux; \
  if ! getent group ${GID} >/dev/null; then \
  groupadd -g ${GID} app; \
  fi; \
  if ! getent passwd ${UID} >/dev/null; then \
  useradd -m -u ${UID} -g ${GID} app; \
  fi

WORKDIR /app

# ---- tooling ----
RUN go install github.com/air-verse/air@latest \
  & chown -R ${UID}:${GID} /go /app

USER ${UID}:${GID}

CMD ["air"]
