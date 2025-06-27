FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN adduser -D -s /bin/sh spring
USER spring

COPY --chown=spring:spring build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]