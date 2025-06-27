#ARM64 최적화 + 캐시 최적화 Dockerfile (호환성 개선)
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

#Gradle Wrapper와 설정 파일만 먼저 복사 (캐시 최적화)
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

#Gradle 실행 권한 부여 (Alpine에서 필요)
RUN chmod +x ./gradlew

#종속성 다운로드 (이 레이어는 build.gradle 변경시에만 재실행)
RUN ./gradlew dependencies --no-daemon

#3단계: 소스코드 복사 및 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

#4단계: ARM64 Alpine 런타임 (60% 용량 절약)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

#Alpine 최적화: 타임존 설정
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

#보안 강화: non-root 사용자 생성
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -G spring
USER spring:spring

#JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

#JVM 최적화 옵션 (M1 Air 8GB 기준)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]