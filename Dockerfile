FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q -DskipTests=true dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests=true package

FROM eclipse-temurin:21-jre
ENV TZ=UTC \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+AlwaysActAsServerClassMachine -XX:+ExitOnOutOfMemoryError -Duser.timezone=UTC"
WORKDIR /opt/app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]