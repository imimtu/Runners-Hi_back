FROM gradle:7.6-jdk17 as build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]