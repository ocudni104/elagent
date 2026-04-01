FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

ARG NODE_MAJOR=22
ARG ATLAS_VERSION=0.30.0

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bash \
        ca-certificates \
        curl \
        docker.io \
        git \
        gnupg \
        less \
        postgresql-client \
        procps \
        ripgrep \
        software-properties-common \
        unzip \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key \
        | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_${NODE_MAJOR}.x nodistro main" \
        > /etc/apt/sources.list.d/nodesource.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

RUN ARCH="$(dpkg --print-architecture)" \
    && case "${ARCH}" in \
        amd64) ATLAS_ARCH="linux_amd64" ;; \
        arm64) ATLAS_ARCH="linux_arm64" ;; \
        *) echo "Unsupported architecture: ${ARCH}" >&2; exit 1 ;; \
    esac \
    && curl -fsSL "https://release.ariga.io/atlas/atlas-linux-${ATLAS_ARCH#linux_}-v${ATLAS_VERSION}.gz" \
        | gunzip -c > /usr/local/bin/atlas \
    && chmod +x /usr/local/bin/atlas

RUN ARCH="$(dpkg --print-architecture)" \
    && case "${ARCH}" in \
        amd64) JDK_ARCH="x64" ;; \
        arm64) JDK_ARCH="aarch64" ;; \
        *) echo "Unsupported architecture: ${ARCH}" >&2; exit 1 ;; \
    esac \
    && curl -fsSL "https://api.adoptium.net/v3/binary/latest/25/ga/linux/${JDK_ARCH}/jdk/hotspot/normal/eclipse?project=jdk" \
        -o /tmp/jdk25.tar.gz \
    && mkdir -p /opt/jdk \
    && tar -xzf /tmp/jdk25.tar.gz -C /opt/jdk --strip-components=1 \
    && rm -f /tmp/jdk25.tar.gz

ENV JAVA_HOME=/opt/jdk
ENV PATH=${JAVA_HOME}/bin:${PATH}

RUN corepack enable && corepack prepare pnpm@latest --activate

USER vscode

