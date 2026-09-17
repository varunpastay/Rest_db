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

# The app stores every timestamp (orders, sessions, invoices) using the container's
# wall-clock time with no timezone conversion anywhere in the code - so the container's
# own clock must already be India time, or every "time ago" on the dashboard and every
# printed invoice time is off by exactly the UTC/IST gap (5h30m).
ENV TZ=Asia/Kolkata

# Render sets $PORT at runtime; application.properties already reads it via server.port=${PORT:8083}
EXPOSE 8083
ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "/app/app.jar"]
