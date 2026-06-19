# 阶段 1：构建阶段（包含测试）
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
# 复制 pom.xml 和源码
COPY pom.xml .
COPY src ./src
# 执行打包（包含测试，失败时构建中断）
RUN mvn clean package

# 阶段 2：运行阶段
# 使用 eclipse-temurin 替代已废弃的 openjdk 镜像
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 从构建阶段复制打包好的 JAR
COPY --from=build /app/target/*.jar app.jar
# 暴露后端端口
EXPOSE 8080
# 启动指令
ENTRYPOINT ["java", "-jar", "app.jar"]
