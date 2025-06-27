# ARM64 최적화 + 캐시 최적화 Dockerfile
FROM --platform=linux/arm64 gradle:8-jdk17-alpine as build

WORKDIR /app

# Gradle Wrapper와 설정 파일만 먼저 복사 (캐시 최적화)
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# 종속성 다운로드 (이 레이어는 build.gradle 변경시에만 재실행)
RUN ./gradlew dependencies --no-daemon

# 소스코드 복사 및 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# ARM64 Alpine 런타임 (60% 용량 절약)
FROM --platform=linux/arm64 eclipse-temurin:17-jre-alpine

WORKDIR /app

# Alpine 최적화: 타임존 설정
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# 보안 강화: non-root 사용자 생성
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -G spring
USER spring:spring

# JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# JVM 최적화 옵션 (M1 Air 8GB 기준)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]