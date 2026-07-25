# QuickServe Backend Dockerfile
FROM registry.redhat.io/ubi9/openjdk-21-runtime:latest

# Create non-root user
USER root
RUN useradd -m -u 1001 quickserve
WORKDIR /app

# Copy built JAR
COPY target/quickserve-erp-*.jar app.jar

# Set ownership
RUN chown quickserve:quickserve app.jar

# Switch to non-root user
USER 1001

# JVM settings for ARM64 / low-memory VPS
ENV JAVA_OPTS="-Xms256m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+OptimizeStringConcat"

# Virtual thread support (Java 21)
ENV JAVA_TOOL_OPTIONS="--enable-preview"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
