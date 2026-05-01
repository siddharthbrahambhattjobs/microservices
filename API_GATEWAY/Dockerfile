# =============================================================================
# MULTI-STAGE DOCKERFILE — Spring Boot Microservice (Java 21)
#
# Strategy: Two-stage build
#   Stage 1 (builder)  — Unpacks the fat JAR into Spring Boot layers
#   Stage 2 (runtime)  — Lean JRE image with only what's needed to run
#
# Why layers matter:
#   Spring Boot's layertools splits the JAR into:
#     1. dependencies         → 3rd-party libs  (changes rarely)
#     2. spring-boot-loader   → Boot internals   (changes rarely)
#     3. snapshot-dependencies→ SNAPSHOT libs    (changes sometimes)
#     4. application          → YOUR code        (changes every build)
#   Docker caches each COPY as a separate layer — so rebuilds only
#   re-upload the changed layer instead of the full fat JAR (~100 MB saved).
# =============================================================================


# =============================================================================
# Stage 1: BUILDER
# Purpose: Unpack the fat JAR into cacheable layers
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /builder

# ARG lets CI/CD override the JAR path at build time:
#   docker build --build-arg JAR_FILE=target/my-service-1.0.jar .
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar

# Use Spring Boot's built-in layertools to extract the JAR into folders.
# Output folders inside /builder:
#   /builder/dependencies/
#   /builder/spring-boot-loader/
#   /builder/snapshot-dependencies/
#   /builder/application/
RUN java -Djarmode=layertools -jar application.jar extract --destination extracted


# =============================================================================
# Stage 2: RUNTIME
# Purpose: Minimal, secure image that actually runs the service
#
# eclipse-temurin:21-jre-alpine is chosen because:
#   - JRE (not JDK) → no compiler, no jshell → smaller attack surface
#   - Alpine base    → ~7 MB OS vs ~80 MB for Debian/Ubuntu variants
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

# ---------------------------------------------------------------------------
# Image metadata (OCI standard labels)
# Useful for: Docker Hub, Harbor, AWS ECR, automated audits
# Override at build time: --label org.opencontainers.image.version=2.1.0
# ---------------------------------------------------------------------------
LABEL org.opencontainers.image.title="shopnest-service" \
      org.opencontainers.image.description="ShopNest Spring Boot Microservice" \
      org.opencontainers.image.vendor="ShopNest" \
      org.opencontainers.image.base.name="eclipse-temurin:21-jre-alpine"

WORKDIR /app

# ---------------------------------------------------------------------------
# Install tini — a minimal init system (PID 1 replacement)
#
# Problem without tini:
#   ENTRYPOINT ["sh", "-c", "java ..."] → sh is PID 1, java is a child.
#   When Kubernetes sends SIGTERM (graceful shutdown), sh receives it but
#   does NOT forward it to java → your app never shuts down gracefully →
#   Kubernetes force-kills after the terminationGracePeriodSeconds.
#
# With tini:
#   tini becomes PID 1, forwards all signals to java correctly, and also
#   reaps zombie processes (important in sidecar/multi-process scenarios).
# ---------------------------------------------------------------------------
RUN apk add --no-cache tini

# ---------------------------------------------------------------------------
# Security: Run as a non-root user
#
# Why this matters:
#   - If the app is compromised, the attacker gets only microservice's
#     limited privileges, not root inside the container.
#   - Required by many Kubernetes PodSecurityPolicy / OPA rules.
#   - Best practice per CIS Docker Benchmark.
#
# NOTE: User is created BEFORE the COPY steps so we can use --chown below.
# ---------------------------------------------------------------------------
RUN addgroup -S microservice && adduser -S microservice -G microservice

# ---------------------------------------------------------------------------
# Copy extracted layers in order of least → most frequently changed
#
# --chown=microservice:microservice  ← CRITICAL fix from original Dockerfile
#   Without this, all files are owned by root even though USER is microservice.
#   The JVM may fail to read class files or write temp files at runtime.
#
# Layer cache behaviour:
#   - dependencies + spring-boot-loader → cached unless pom.xml changes
#   - snapshot-dependencies            → cached unless SNAPSHOT version bumps
#   - application                      → re-copied on every code change
# ---------------------------------------------------------------------------
COPY --from=builder --chown=microservice:microservice /builder/extracted/dependencies/          ./
COPY --from=builder --chown=microservice:microservice /builder/extracted/spring-boot-loader/    ./
COPY --from=builder --chown=microservice:microservice /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=microservice:microservice /builder/extracted/application/           ./

# Switch to the non-root user for all subsequent instructions and at runtime
USER microservice:microservice

# ---------------------------------------------------------------------------
# Port declaration
# EXPOSE is documentation only — it does NOT publish the port.
# Publishing happens at: docker run -p 8151:8151 or in docker-compose/K8s.
# ---------------------------------------------------------------------------
EXPOSE 8151

# ---------------------------------------------------------------------------
# Spring Boot runtime configuration
#
# SPRING_PROFILES_ACTIVE:
#   Override at deploy time for different environments:
#     docker run -e SPRING_PROFILES_ACTIVE=prod ...
#   Default is "default" (Spring's baseline profile).
#
# SERVER_PORT:
#   Mirrors the EXPOSE port. Can be overridden without rebuilding the image.
# ---------------------------------------------------------------------------
ENV SPRING_PROFILES_ACTIVE="default" \
    SERVER_PORT=8151

# ---------------------------------------------------------------------------
# JVM tuning for containerized Java 21
#
# -XX:+UseContainerSupport
#   Tells the JVM to read cgroup memory/CPU limits (set by Docker/K8s)
#   instead of the host machine's total resources.
#   Without this (Java < 10), the JVM sees host RAM → sets heap too large
#   → container OOM-killed by the kernel.
#
# -XX:MaxRAMPercentage=75.0
#   Heap = 75% of the container's memory limit.
#   Leaves 25% for: OS, metaspace, thread stacks, JVM internals, GC buffers.
#   Example: 512 MB container → ~384 MB heap.
#
# -XX:InitialRAMPercentage=50.0
#   Start heap at 50% so the JVM doesn't grab all memory at cold start.
#   Helps in environments where containers are packed tightly.
#
# -XX:+UseZGC -XX:+ZGenerational
#   ZGC is Java 21's production-grade low-latency GC.
#   ZGenerational (new in 21) adds generational collection to ZGC →
#   better throughput for mixed short/long-lived object workloads (typical
#   for REST microservices). Sub-millisecond GC pauses even at large heaps.
#
# -XX:+ExitOnOutOfMemoryError
#   If the JVM runs out of heap, it exits immediately instead of hanging.
#   This lets Kubernetes detect the crash and restart the pod cleanly,
#   rather than leaving a zombie process that appears healthy.
#
# -XX:MaxMetaspaceSize=256m
#   Caps metaspace (class metadata, reflection, proxies).
#   Without a cap, metaspace can silently grow and eat container memory.
#   256 MB is generous for most Spring Boot services.
#
# -Djava.security.egd=file:/dev/./urandom
#   Tells the JVM to use /dev/urandom for SecureRandom seeding.
#   Default /dev/random can block waiting for entropy in containers →
#   slow startup (especially noticeable in Spring Security / HTTPS).
#   The odd path /dev/./urandom bypasses an old JVM hotspot that ignores
#   plain /dev/urandom and falls back to /dev/random anyway.
# ---------------------------------------------------------------------------
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:+ExitOnOutOfMemoryError \
  -XX:MaxMetaspaceSize=256m \
  -Djava.security.egd=file:/dev/./urandom"

# ---------------------------------------------------------------------------
# Health Check
#
# Docker (and docker-compose) uses this to mark the container as healthy.
# Kubernetes uses its own liveness/readiness probes, but this is useful
# for local dev and docker-compose-based integration tests.
#
# --interval=30s  → check every 30 seconds
# --timeout=5s    → fail if no response within 5 seconds
# --start-period=60s → give Spring Boot 60s to start before checking
#                      (avoids false restarts during startup)
# --retries=3     → mark unhealthy only after 3 consecutive failures
#
# /actuator/health requires spring-boot-starter-actuator on the classpath.
# If not used, replace with: CMD ["sh", "-c", "nc -z localhost $SERVER_PORT"]
# ---------------------------------------------------------------------------
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider \
      http://localhost:${SERVER_PORT}/actuator/health || exit 1

# ---------------------------------------------------------------------------
# Entrypoint
#
# tini -g -- java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher
#
# tini -g:
#   Runs tini as a process group init. The -g flag tells tini to send
#   signals to the entire process group, not just the direct child.
#
# org.springframework.boot.loader.launch.JarLauncher:
#   This is the Spring Boot 3.x launcher main class (changed from
#   org.springframework.boot.loader.JarLauncher in Boot 2.x).
#   It reads BOOT-INF/classes and BOOT-INF/lib from the extracted
#   layer folders — much faster than re-inflating the fat JAR.
#
# Why not ENTRYPOINT ["java", ...]?
#   Using exec form directly with java and $JAVA_OPTS is tricky because
#   exec form does NOT expand shell variables. The sh -c wrapper lets
#   $JAVA_OPTS expand correctly at runtime, while tini handles PID 1 duties.
# ---------------------------------------------------------------------------
ENTRYPOINT ["/sbin/tini", "-g", "--"]
CMD ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]