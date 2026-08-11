FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY target/campus-gateway-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Dspring.cloud.nacos.config.server-addr=nacos:8848", "-Dspring.cloud.nacos.discovery.server-addr=nacos:8848", "-Dspring.cloud.nacos.config.fail-fast=false", "-Dspring.cloud.nacos.discovery.fail-fast=false", "-jar", "app.jar"]