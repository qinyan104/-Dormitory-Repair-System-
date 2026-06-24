# 阶段 1：构建阶段（层缓存优化）
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# 先复制 pom.xml，利用 Docker 层缓存避免重复下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并打包
COPY src ./src
RUN mvn clean package -DskipTests

# 单独运行测试
RUN mvn test

# 阶段 2：运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 安装 curl（用于健康检查）
RUN apk add --no-cache curl

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
