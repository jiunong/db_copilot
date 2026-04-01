# 使用 JDK 17 作为基础镜像
FROM hzkjhub/java17:17.0.4

# 设置工作目录
WORKDIR /app

# 创建配置和日志目录
RUN mkdir -p /app/config /app/logs

# 复制 jar 包到容器中
COPY target/db_copilot-0.0.1-SNAPSHOT.jar /app/app.jar

# 复制默认配置模板（避免被挂载卷直接覆盖）
COPY src/main/resources/application.yml /app/application.yml.template

# 写入启动脚本：在容器启动时初始化配置文件
RUN cat > /app/entrypoint.sh <<'EOF'
#!/bin/sh
set -e

mkdir -p /app/config /app/logs

if [ ! -f /app/config/application.yml ]; then
	cp /app/application.yml.template /app/config/application.yml
fi

if [ ! -f /app/config/db-copilot-config.json ]; then
	echo "[]" > /app/config/db-copilot-config.json
fi

exec java $JAVA_OPTS \
	-Djava.security.egd=file:/dev/./urandom \
	-Dspring.config.location=/app/config/application.yml \
	-jar /app/app.jar
EOF

RUN chmod +x /app/entrypoint.sh

# 暴露应用端口
EXPOSE 18080

# 挂载卷: 配置文件和日志
VOLUME ["/app/config", "/app/logs"]

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# 启动应用
ENTRYPOINT ["/app/entrypoint.sh"]
