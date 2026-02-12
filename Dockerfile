FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew

ENTRYPOINT ["./gradlew", "bootRun", "--args=--spring.profiles.active=docker"]
