FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Python + edge-tts CLI for TTS support
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip \
    && pip3 install --no-cache-dir edge-tts \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENV MOCK_LLM_ENABLED=true
ENV EMBEDDING_PROVIDER=mock
ENV VECTOR_STORE=in-memory
ENTRYPOINT ["java", "-jar", "app.jar"]
