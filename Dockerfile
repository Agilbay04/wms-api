FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=builder --chown=spring:spring /workspace/build/libs/*.jar app.jar
RUN mkdir /app/exports /app/seed-results && chown -R spring:spring /app

USER spring
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
