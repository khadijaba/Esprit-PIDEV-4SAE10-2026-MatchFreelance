FROM eclipse-temurin:17-jre

EXPOSE 8088

ADD target/contract-service-0.0.1-SNAPSHOT.jar contract-service.jar

ENTRYPOINT ["java","-jar","contract-service.jar"]

