# Стадия 1 Сборка
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Копирую pom.xml и зависимости
COPY pom.xml .
RUN mvn dependency:go-offline -B

# копирую исходники + собир jar
COPY src ./src
RUN mvn clean package -DskipTests

# Стадия 2 Рантайм
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Копирую готовый jar
COPY --from=builder /app/target/UserService-0.0.1-SNAPSHOT.jar app.jar

# Указываю профиль docker
ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]