# ---------- Stage 1: build the jar with Maven (only this stage has the JDK + Maven cache) ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy the pom first so dependency downloads are cached between builds when only code changes
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- Stage 2: small runtime image (JRE only, no build tools, no source code) ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Free hosts give 512 MB RAM. Heap is sized from the CONTAINER limit (55% ~ 280 MB), leaving room for metaspace,
# thread stacks and the Mongo/Redis/Netty buffers, so the host never kills the process for exceeding memory.
# SerialGC + C1-only JIT use the least memory and start fastest on the free 0.1 CPU.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55 -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
# Run as a non-root user (least privilege)
RUN useradd --system --uid 10001 nexus
USER nexus
ENTRYPOINT ["java", "-jar", "app.jar"]
