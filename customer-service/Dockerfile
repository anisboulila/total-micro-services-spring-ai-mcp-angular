FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml pom.xml
COPY customer-service/pom.xml customer-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY customer-service/src customer-service/src

RUN mvn -B -pl customer-service -am package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build --chown=10001:10001 /workspace/customer-service/target/customer-service-*.jar /app/app.jar

USER 10001:10001

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
