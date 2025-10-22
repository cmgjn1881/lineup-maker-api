# 1단계: 빌드 스테이지 - Gradle과 JDK를 포함한 이미지 사용
FROM gradle:jdk17-jammy AS build
# 작업 디렉토리 설정: 이미지를 만든 사용자(gradle)의 홈 디렉토리 내에 프로젝트 폴더를 만듭니다.
WORKDIR /home/gradle/lineup-maker-api

# 소스 파일 복사: Gradle 빌드에 필요한 모든 파일(gradlew, 설정 파일, 소스)을 작업 디렉토리로 복사합니다.
# 주의: 이 프로젝트 구조가 Render Git 저장소의 루트와 일치해야 합니다.
COPY --chown=gradle:gradle . .

# Gradle Build 실행: bootJar를 사용하여 실행 가능한 JAR 파일을 생성합니다.
# -x test 옵션은 빌드 시 테스트를 생략하여 빌드 시간을 단축합니다.
RUN ./gradlew bootJar --no-daemon -x test
# 빌드된 JAR 파일은 /home/gradle/lineup-maker-api/build/libs/ 에 위치하게 됩니다.

# 2단계: 패키징 및 실행 스테이지 (더 가벼운 JRE 이미지 사용)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일을 최종 실행 환경으로 복사합니다.
# **주의: 파일명은 프로젝트에 맞게 수정해야 할 수 있습니다.**
# 예시: lineup-maker-api-0.0.1-SNAPSHOT.jar
# 정확한 파일명을 모르면 *.jar를 사용합니다.
COPY --from=build /home/gradle/lineup-maker-api/build/libs/*.jar app.jar

# Spring Boot 포트 설정
ENV SERVER_PORT 8080
EXPOSE 8080

# 애플리케이션 실행 명령어
# 이 명령어가 Render에서 서비스 시작 명령 역할을 합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]