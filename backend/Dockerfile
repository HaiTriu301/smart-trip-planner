# Backend image (design.md 17.1). Build from the backend/ folder:  docker build -t <name> .
# Two stages: the first has the JDK and Gradle and builds the jar, the second only has a Java runtime and the
# built application. What the first stage downloads never reaches the image that runs on the server.

# ---------- Stage 1: build the jar ----------
# Same Gradle version as the wrapper (gradle/wrapper/gradle-wrapper.properties)
FROM gradle:9.7.1-jdk21 AS build
WORKDIR /workspace

COPY settings.gradle build.gradle ./
COPY gradle/libs.versions.toml gradle/
COPY src/ src/

# - Tests do not run here: CI runs them on every pull request (they need Docker for Testcontainers).
# - No separate step to pre-download the libraries: "gradle dependencies" only fetches their descriptions, not the
#   jars, and keeping Gradle's download folder between builds saved nothing measurable (72.5 s vs 72.0 s, compiling
#   is what takes the time). Measured on 2026-10-21.
# - The jar is then split into layers: libraries, Spring Boot loader, application code. Each one becomes a layer of
#   the image, so a deploy that only changes code downloads the small application layer, not 80 MB of libraries
RUN gradle bootJar --no-daemon --quiet \
 && cp "$(find build/libs -name '*.jar' ! -name '*-plain.jar')" application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------- Stage 2: run ----------
FROM eclipse-temurin:21-jre-alpine

# Run as an ordinary user: a flaw in the application does not hand out root inside the container.
# The files stay owned by root, so the application cannot change its own code either
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

# Least changed first: Docker reuses a layer only while every layer before it is unchanged
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app
EXPOSE 8080

# Heap at most 60% of the memory the container is allowed (docker-compose.prod.yml): Java needs the rest outside the
# heap. Out of memory: stop at once and let Docker restart the container, instead of limping on
# (WORKFLOW Task 4.8, decision 11). Compose can replace the whole value
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError"

# Docker asks every 30 s whether the application answers UP; three failures in a row mark the container unhealthy.
# The first 2 minutes do not count: Flyway and Spring start slowly on a small machine
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "application.jar"]
