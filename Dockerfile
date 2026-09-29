FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17-alpine AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist/frontend/browser ./src/main/resources/static
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
LABEL org.opencontainers.image.source="https://github.com/MontassarBenMesmia/schemaflow-platform" \
      org.opencontainers.image.description="Relational-to-JSON analytics modernization platform" \
      org.opencontainers.image.licenses="MIT"
RUN addgroup -S schemaflow && adduser -S schemaflow -G schemaflow
WORKDIR /app
COPY --from=backend-build /workspace/backend/target/schemaflow-api-1.0.0.jar app.jar
USER schemaflow
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=4s --start-period=25s --retries=3 \
  CMD wget --spider -q "http://localhost:${PORT:-8080}/api/v1/health" || exit 1
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT:-8080} -jar app.jar"]
