FROM node:22.23.4-alpine AS frontend-build
RUN apk add --no-cache --upgrade ca-certificates
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17-alpine AS backend-build
RUN apk add --no-cache --upgrade ca-certificates
RUN for alias in \
      "cn_emsign_ecc_root_ca__c3,o_emudhra_inc,ou_emsign_pki,c_us [jdk]" \
      "cn_emsign_ecc_root_ca__g3,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]" \
      "cn_emsign_root_ca__c1,o_emudhra_inc,ou_emsign_pki,c_us [jdk]" \
      "cn_emsign_root_ca__g1,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]"; do \
      if keytool -list -cacerts -storepass changeit -alias "$alias" >/dev/null 2>&1; then \
        keytool -delete -cacerts -storepass changeit -alias "$alias"; \
      fi; \
    done \
    && ! keytool -list -cacerts -storepass changeit | grep -qi emudhra
WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist/frontend/browser ./src/main/resources/static
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache --upgrade ca-certificates
RUN for alias in \
      "cn_emsign_ecc_root_ca__c3,o_emudhra_inc,ou_emsign_pki,c_us [jdk]" \
      "cn_emsign_ecc_root_ca__g3,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]" \
      "cn_emsign_root_ca__c1,o_emudhra_inc,ou_emsign_pki,c_us [jdk]" \
      "cn_emsign_root_ca__g1,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]"; do \
      if keytool -list -cacerts -storepass changeit -alias "$alias" >/dev/null 2>&1; then \
        keytool -delete -cacerts -storepass changeit -alias "$alias"; \
      fi; \
    done \
    && ! keytool -list -cacerts -storepass changeit | grep -qi emudhra
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
