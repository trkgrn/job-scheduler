# ---- Build Stage ----
FROM gradle:8.8-jdk21-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY dependencies.gradle .

COPY app ./app
COPY modules ./modules
COPY platform ./platform

RUN gradle build -x test --no-daemon

# ---- Run Stage ----
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

COPY --from=build /app/app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
