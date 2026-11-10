# ─────────────────────────────────────────────────────────────────────────────
# FundTrack backend: multi-stage build
#   Stage 1 (build): full JDK + Gradle → produces the Spring Boot jar
#   Stage 2 (run):   small JRE + just that jar → the image that actually ships
# ─────────────────────────────────────────────────────────────────────────────

# --platform=$BUILDPLATFORM: compile on the BUILD machine's own chip (fast, no emulation).
# A .jar is the same on every CPU, so it only needs to be built once, even for multi-arch images.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /app

# 1) Copy only the build files first and download dependencies.
#    Docker caches each step: if only Java code changes, this slow step is reused.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

# 2) Copy the source and build the jar.
#    Tests are skipped here: they need Docker (Testcontainers) and run separately (./gradlew test, CI).
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test


# ── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app

# Don't run as root inside the container (least privilege)
RUN useradd --system --uid 1001 spring
USER spring

# Only the jar crosses over: no source code, no Gradle, no JDK in the final image
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
