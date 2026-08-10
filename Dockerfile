# 1단계: Builder Stage - 빌드를 전담하는 환경
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 빌드에 필요한 파일들만 먼저 복사 (캐시 효율화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 라이브러리 미리 다운로드 (코드가 바뀌어도 이 단계는 캐싱)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 JAR 빌드
COPY src src
RUN ./gradlew clean build -x test --no-daemon

# 2단계: Run Stage - 실행만 전담하는 가벼운 환경
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 단계에서 생성된 최종 JAR 파일만 빼오기
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080

# 컨테이너 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
