# 使用轻量级的 JDK 21 镜像
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# 假设你已经手动执行了 mvn clean package，将 target 下的 jar 包拷进去
# 注意：构建前请确保你的 application.yml 里的 redis host 写的是 "redis"，mysql url 写的是 "mysql"
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar
# 暴露 8080 端口
EXPOSE 8080
# 启动 Java 应用
ENTRYPOINT ["java", "-jar", "app.jar"]
