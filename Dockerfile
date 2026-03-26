# 使用 JDK 1.8 作为基础镜像
FROM openjdk:8-jdk-alpine

# 设置工作目录
WORKDIR /app

# 创建配置文件目录
RUN mkdir -p /app/config

# 复制 jar 包到容器中
COPY target/db_copilot-0.0.1-SNAPSHOT.jar /app/app.jar

# 复制配置文件到容器中(作为默认配置)
COPY src/main/resources/application.yml /app/config/application.yml

# 暴露应用端口
EXPOSE 18080

# 设置 JVM 参数(根据需要调整)
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# 启动应用,使用外部配置文件
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.config.location=/app/config/application.yml -jar /app/app.jar"]
