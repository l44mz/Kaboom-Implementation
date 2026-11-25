# One image for the whole app: the Flutter web app is served by the Spring Boot backend.
#   https://<host>/          -> the Flutter app
#   https://<host>/api/v1/   -> the backend API
#   wss://<host>/ws          -> live updates (STOMP)

# 1. Build the Flutter web app.
#    API_BASE_URL=/api/v1 means "the same server that sent the page".
#    Flutter is pinned to the version the team uses (pubspec needs Dart ^3.13.4).
FROM debian:bookworm-slim AS web
RUN apt-get update \
    && apt-get install -y --no-install-recommends git curl unzip xz-utils ca-certificates \
    && rm -rf /var/lib/apt/lists/*
ARG FLUTTER_VERSION=3.47.8
RUN git clone --depth 1 --branch ${FLUTTER_VERSION} https://github.com/flutter/flutter.git /flutter
ENV PATH="/flutter/bin:${PATH}"
RUN flutter config --no-analytics && flutter precache --web
WORKDIR /web
COPY frontend/ .
RUN flutter pub get && flutter build web --release --dart-define=API_BASE_URL=/api/v1

# 2. Build the backend, with the web app inside it (Spring Boot serves /static).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
COPY --from=web /web/build/web ./src/main/resources/static
RUN mvn clean package -DskipTests

# 3. Run it.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
