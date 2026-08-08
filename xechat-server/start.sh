#!/bin/bash
# XEChat Server 启动脚本（nohup 模式）
# 部署路径：/opt/apps/xechat-server

set -e

APP_DIR="/opt/apps/xechat-server"
JAR_FILE="$APP_DIR/xechat-server-2.0.0.jar"
CONFIG_FILE="$APP_DIR/config.setting"
LOG_CONFIG="$APP_DIR/logback.xml"
PID_FILE="$APP_DIR/server.pid"
LOG_DIR="$APP_DIR/logs"
JAVA_OPTS="-Xms256m -Xmx512m"

# 检查 Java
if ! command -v java &>/dev/null; then
    echo "错误: 未找到 java，请安装 JDK 21"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | grep -oP '\d+' | head -1)
if [ "$JAVA_VER" -lt 21 ]; then
    echo "错误: 需要 JDK 21+，当前 $(java -version 2>&1 | head -1)"
    exit 1
fi

case "${1:-start}" in

start)
    if [ -f "$PID_FILE" ] && kill -0 "$(cat $PID_FILE)" 2>/dev/null; then
        echo "xechat-server 已在运行中 (PID: $(cat $PID_FILE))"
        exit 0
    fi

    cd "$APP_DIR"
    mkdir -p "$LOG_DIR"
    echo -n "启动 xechat-server ... "

    nohup java $JAVA_OPTS \
        -Dlogback.configurationFile="$LOG_CONFIG" \
        -jar "$JAR_FILE" \
        -path "$CONFIG_FILE" \
        > "$LOG_DIR/nohup.log" 2>&1 &

    echo $! > "$PID_FILE"
    sleep 1
    if kill -0 "$(cat $PID_FILE)" 2>/dev/null; then
        echo "完成 (PID: $(cat $PID_FILE))"
    else
        echo "失败，请查看 $LOG_DIR/nohup.log"
        rm -f "$PID_FILE"
        exit 1
    fi
    ;;

stop)
    if [ ! -f "$PID_FILE" ]; then
        echo "xechat-server 未运行（无 PID 文件）"
        exit 0
    fi
    PID=$(cat $PID_FILE)
    echo -n "停止 xechat-server (PID: $PID) ... "
    kill "$PID" 2>/dev/null || true
    # 等 10 秒，未退出则强杀
    for i in $(seq 1 10); do
        kill -0 "$PID" 2>/dev/null || break
        sleep 1
    done
    if kill -0 "$PID" 2>/dev/null; then
        echo "超时，强制终止"
        kill -9 "$PID" 2>/dev/null || true
    fi
    rm -f "$PID_FILE"
    echo "已停止"
    ;;

restart)
    "$0" stop
    sleep 1
    "$0" start
    ;;

status)
    if [ -f "$PID_FILE" ] && kill -0 "$(cat $PID_FILE)" 2>/dev/null; then
        echo "xechat-server 运行中 (PID: $(cat $PID_FILE))"
    else
        echo "xechat-server 未运行"
        [ -f "$PID_FILE" ] && echo "(过期的 PID 文件存在)" && rm -f "$PID_FILE"
    fi
    ;;

*)
    echo "用法: $0 {start|stop|restart|status}"
    exit 1
    ;;
esac
