FROM --platform=linux/arm64 gradle:8-jdk17-alpine as build
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./


RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew bootJar --no-daemon


FROM --platform=linux/arm64 eclipse-temurin:17-jre-alpine
WORKDIR /app


RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul


RUN addgroup -g 1001 -S spring && \
    adduser -S spring -G spring
USER spring:spring


COPY --from=build /app/build/libs/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]