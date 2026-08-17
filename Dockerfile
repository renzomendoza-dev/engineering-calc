# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy just the POMs first so dependency resolution is cached in its own layer, then only
# re-runs when a pom.xml actually changes rather than on every source edit.
COPY pom.xml .
COPY calc-core/pom.xml calc-core/pom.xml
COPY calc-api/pom.xml calc-api/pom.xml
RUN mvn -B -q -pl calc-core,calc-api dependency:go-offline

COPY calc-core calc-core
COPY calc-api calc-api
# Builds calc-core then calc-api in one reactor pass, so calc-api's dependency on calc-core
# resolves from this build rather than needing it pre-installed to a repository.
RUN mvn -B -q -pl calc-core,calc-api -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /build/calc-api/target/calc-api-*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
