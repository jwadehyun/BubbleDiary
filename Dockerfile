# Builds one image with the server and the web front end. Run it with: docker compose up --build

# 1. Web front end, built into src/main/resources/static so the server serves it.
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. The server jar. Tests are skipped here because CI runs them (they need Docker themselves).
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml ./
COPY src ./src
COPY --from=frontend /app/src/main/resources/static ./src/main/resources/static
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# 3. A small runtime image with just Java and the jar, running as a non-root user.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=backend /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
