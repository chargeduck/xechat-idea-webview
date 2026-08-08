FROM eclipse-temurin:21-jre

WORKDIR /opt/apps/xechat-server

# 胖 jar（maven-assembly-plugin 产物）
# 胖 jar（maven-assembly-plugin 产物，保留原始文件名）
COPY target/xechat-server-2.0.0.jar .

# 外部配置（覆盖 classpath 默认值，ip2Region_path 已指向本目录）
COPY config.setting .

# 外部日志配置（日志输出到 ./logs/）
COPY logback.xml .

# 运行时数据文件（ip2region 地址库、敏感词库）
COPY src/main/resources/db/ data/db/

RUN mkdir -p logs upload

EXPOSE 1024 1025

# data：ip2region.xdb / keywords.txt    logs：日志    upload：用户上传文件
VOLUME ["/opt/apps/xechat-server/data", "/opt/apps/xechat-server/logs", "/opt/apps/xechat-server/upload"]

# -Dlogback.configurationFile 使用外部日志配置，-path 使用外部 server 配置
ENTRYPOINT ["java", \
    "-Dlogback.configurationFile=logback.xml", \
    "-jar", "xechat-server-2.0.0.jar", \
    "-path", "config.setting"]
