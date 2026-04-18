FROM eclipse-temurin:21-jdk-jammy

ARG UID=1000
ARG GID=1000

WORKDIR /workspace

# install minimal tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    git curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
  if ! getent group ${GID} >/dev/null; then \
    groupadd -g ${GID} app; \
  fi; \
  if ! getent passwd ${UID} >/dev/null; then \
    useradd -m -u ${UID} -g ${GID} app; \
  fi

ENV HOME=/home/app

RUN mkdir -p /home/app /workspace \
 && chown -R ${UID}:${GID} /home/app /workspace

USER ${UID}

CMD ["sleep","infinity"]
