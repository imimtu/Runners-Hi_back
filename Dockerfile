#FROM gradle:7.6-jdk17-alpine as build
#WORKDIR /app
#COPY . .
#RUN gradle bootJar --no-daemon
#
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
#COPY --from=build /app/build/libs/*.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]

FROM gradle:7.6-jdk17-alpine as build
WORKDIR /app

COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle/ ./gradle/

RUN gradle dependencies --no-daemon --quiet

COPY src/ ./src/
RUN gradle bootJar --no-daemon --quiet

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]