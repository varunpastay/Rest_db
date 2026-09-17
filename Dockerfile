# --- Build stage: compile with Maven + JDK 17, cache dependencies separately from source ---
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B clean package -DskipTests

# --- Run stage: slim JRE only, no Maven/JDK left in the final image ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/restaurant-ordering-system-*.jar app.jar

# Render sets $PORT at runtime; application.properties already reads it via server.port=${PORT:8083}
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
