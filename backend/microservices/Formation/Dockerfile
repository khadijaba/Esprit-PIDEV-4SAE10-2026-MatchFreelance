FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY src src
RUN mvn -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 8081
ENV DB_HOST=mysql-db
ENV DB_PORT=3306
ENV EUREKA_HOST=eureka-server
ENV EUREKA_PORT=8761
COPY --from=build /workspace/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
