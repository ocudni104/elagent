FROM eclipse-temurin:25-jdk-jammy

WORKDIR /workspace

# install minimal tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    git curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

CMD ["sleep","infinity"]
