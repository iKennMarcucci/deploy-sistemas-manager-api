#############################
# ETAPA 1: “builder” con Maven
#############################
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos pom.xml, mvnw, .mvn/ y el código fuente (src/)
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src

# Damos permisos y compilamos el JAR sin tests
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

#############################
# ETAPA 2: runtime con OpenJDK
#############################
FROM openjdk:17-jdk-alpine

WORKDIR /app

# Copiamos el JAR que Maven generó en la etapa “builder”
COPY --from=builder /app/target/edu_virtual_ufps-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto que usa Spring Boot
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]
