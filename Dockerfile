FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=builder /app/target/parser-1.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]