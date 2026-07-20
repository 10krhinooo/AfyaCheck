# Backend image: Node stage builds the SPA, Gradle stage bundles it into the boot jar
# (processResources picks up the prebuilt frontend/dist even with -PskipFrontend), JRE
# stage runs it. Used by docker-compose's "full" profile.
FROM node:20-slim AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN ./gradlew --version
COPY src/ src/
COPY --from=frontend /app/frontend/dist frontend/dist
RUN ./gradlew bootJar -PskipFrontend --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/afyacheck.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
