# 1단계: 빌드 스테이지 - Gradle과 JDK를 포함한 이미지 사용
FROM gradle:8.8-jdk17-alpine AS build
WORKDIR /app

# Gradle Wrapper 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 소스 코드 복사
COPY src src

# Gradle Wrapper 실행 권한 부여
RUN chmod +x gradlew

# JAR 파일 빌드 (테스트 생략)
RUN ./gradlew bootJar --no-daemon -x test

# 2단계: 패키징 및 실행 스테이지 (더 가벼운 JRE 이미지 사용)
FROM openjdk:17-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Spring Boot 포트 설정 (Render는 내부적으로 10000 포트를 사용하도록 권장하지만, 8080도 작동하며, Dockerfile에 EXPOSE 8080을 명시하는 것이 일반적입니다.)
ENV SERVER_PORT 8080
EXPOSE 8080

# 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]