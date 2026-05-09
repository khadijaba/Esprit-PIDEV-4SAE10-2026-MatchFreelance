FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY src src
RUN mvn -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 8050
# Eureka : sur Render utiliser les variables d'environnement (ex. EUREKA_URL), pas le hostname Docker Compose.
COPY --from=build /workspace/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
