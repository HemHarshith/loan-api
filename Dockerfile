# ---------- Stage 1: build the jar ----------
# A full JDK is needed to compile. This stage is thrown away afterwards.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only the build files first. Docker caches each step, so the slow
# dependency download is reused until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Remove Windows line endings (they break the script on Linux) and make it runnable.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q dependency:go-offline

# Now copy the source and build. Tests are skipped here because they run
# in CI (Step 7), and the full-context test needs a database.
COPY src src
RUN ./mvnw -q package -DskipTests

# ---------- Stage 2: the small image we actually run ----------
# Only a JRE (run Java, not compile it), so the final image is smaller.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a normal user, not root, so a bug in the app cannot do as much damage.
RUN useradd --system --create-home appuser

# Copy just the finished jar from stage 1.
COPY --from=build /app/target/*.jar app.jar
USER appuser

# Documentation only: the app listens on 8080.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]