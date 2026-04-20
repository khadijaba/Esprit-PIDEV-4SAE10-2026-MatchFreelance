# Atelier (un seul MS) : depuis ce dossier après mvn clean package -DskipTests
#   docker build -t mf-candidature .
#   docker run -p 8083:8083 mf-candidature
# Équivalent atelier openjdk:17 (openjdk officiel retiré du Hub → Temurin JRE 17)
FROM eclipse-temurin:17-jre-jammy
EXPOSE 8083
ADD target/candidature-service-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
