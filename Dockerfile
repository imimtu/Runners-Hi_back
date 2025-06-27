FROM gradle:7.6-jdk17-alpine AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew bootJar --no-daemon && \
    java -Djarmode=layertools -jar build/libs/*.jar extract

FROM amazoncorretto:17-alpine
RUN apk add --no-cache curl && \
    adduser -D spring && \
    rm -rf /var/cache/apk/*

WORKDIR /app
USER spring

COPY --from=build --chown=spring:spring /app/dependencies/ ./
COPY --from=build --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /app/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /app/application/ ./

ENV JAVA_OPTS="-XX:MaxRAMPercentage=80 -XX:+UseG1GC -XX:+UseContainerSupport"

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]