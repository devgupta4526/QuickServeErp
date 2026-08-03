FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S quickserve && adduser -S quickserve -G quickserve
WORKDIR /app
COPY --from=builder --chown=quickserve:quickserve /build/target/quickserve-erp-*.jar app.jar
USER quickserve

# Sized for a 512 MB free instance.
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
