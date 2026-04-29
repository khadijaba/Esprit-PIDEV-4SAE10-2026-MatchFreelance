FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml ./
# Pré-télécharge les dépendances quand possible (échec toléré : le package suivant les résout).
RUN mvn -q -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/evaluation-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","app.jar"]
