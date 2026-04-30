FROM eclipse-temurin:17-jre AS builder
WORKDIR /workspace
COPY target/Skill-0.0.1-SNAPSHOT.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 8083

# Couches Spring Boot : seule la couche "application" change souvent → push Docker Hub plus léger.
COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
