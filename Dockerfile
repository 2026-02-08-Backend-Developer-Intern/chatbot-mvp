#FROM gradle:8.7-jdk17 AS build

FROM gradle:8.7-jdk17-jammy AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle bootJar --no-daemon -x test

#FROM eclipse-temurin:17-jre-alpine

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
