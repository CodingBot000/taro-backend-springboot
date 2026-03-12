# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

COPY . .

RUN ./gradlew bootJar

# ---------- run stage ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]