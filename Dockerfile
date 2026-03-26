# 使用 JDK 1.8 作为基础镜像
FROM openjdk:8-jdk-alpine

# Set Timezone
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 设置工作目录
WORKDIR /app

# 创建配置和数据目录
RUN mkdir -p /app/config && mkdir -p /app/logs

# 复制 jar 包到容器中
COPY target/db_copilot-0.0.1-SNAPSHOT.jar /app/app.jar

# 复制默认配置文件到容器中 (可选，如果想保留默认值)
COPY src/main/resources/application.yml /app/config/application.yml

# 暴露应用端口
EXPOSE 18080

# 挂载卷: 配置文件和日志
VOLUME ["/app/config", "/app/logs"]

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.config.location=/app/config/application.yml -jar /app/app.jar"]
