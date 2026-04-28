FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 8081
ENV DB_HOST=mysql-db
ENV DB_PORT=3306
ENV EUREKA_HOST=eureka-server
ENV EUREKA_PORT=8761
ADD target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
