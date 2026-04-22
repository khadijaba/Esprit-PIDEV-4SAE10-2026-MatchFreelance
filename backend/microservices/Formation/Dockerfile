FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 8081
ADD target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
