# ============================================================
# 校园闲置平台 - 后端多阶段构建 Dockerfile
# 服务器上只需 Docker，无需本地安装 JDK/Maven：镜像内完成编译
# 构建阶段一次性编译 common + 全部 6 个微服务
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -Dmaven.test.skip=true package

# ============================================================
# 运行阶段：每个微服务对应一个 stage，共用上面编译出的 jar
# 所有运行参数（Nacos/MySQL/Redis/Kafka/OSS/JWT）通过 docker-compose
# 环境变量注入，见各服务 application.yaml 中的 ${VAR:default}
# ============================================================

FROM eclipse-temurin:21-jre-alpine AS campus-gateway
WORKDIR /app
COPY --from=build /workspace/campus-gateway/target/campus-gateway-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-alpine AS campus-auth
WORKDIR /app
COPY --from=build /workspace/campus-auth/target/campus-auth-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-alpine AS campus-user
WORKDIR /app
COPY --from=build /workspace/campus-user/target/campus-user-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-alpine AS campus-item
WORKDIR /app
COPY --from=build /workspace/campus-item/target/campus-item-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-alpine AS campus-order
WORKDIR /app
COPY --from=build /workspace/campus-order/target/campus-order-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-alpine AS campus-admin
WORKDIR /app
COPY --from=build /workspace/campus-admin/target/campus-admin-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
