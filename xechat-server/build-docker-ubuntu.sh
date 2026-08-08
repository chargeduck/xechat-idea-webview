#!/bin/bash
set -e

REPO_URL="https://gitee.com/chargeduck/xechat-idea-webview.git"
WORK_DIR="/tmp/xechat-build"
PROJECT_DIR="$WORK_DIR/xechat-idea-webview"
IMAGE_NAME="xechat-server"
IMAGE_TAG="latest"

echo "============================================"
echo "  XEChat Server Docker 一键构建 (Ubuntu)"
echo "============================================"
echo ""

# ---------- 安装 git ----------
if ! command -v git &>/dev/null; then
    echo "[1/5] 安装 git..."
    apt-get update -qq
    apt-get install -y git
else
    echo "[1/5] git 已安装: $(git --version)"
fi

# ---------- 安装 JDK 21 ----------
if ! command -v javac &>/dev/null || ! javac --version 2>&1 | grep -qE '21[^0-9]'; then
    echo "[2/5] 安装 JDK 21..."
    apt-get update -qq
    if apt-cache show openjdk-21-jdk &>/dev/null 2>&1; then
        apt-get install -y openjdk-21-jdk
    else
        echo "  未找到 openjdk-21-jdk，添加 Adoptium 源..."
        wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public \
            | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg
        UBUNTU_CODENAME=$(lsb_release -cs)
        echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $UBUNTU_CODENAME main" \
            > /etc/apt/sources.list.d/adoptium.list
        apt-get update -qq
        apt-get install -y temurin-21-jdk
    fi
else
    echo "[2/5] JDK 21 已安装: $(javac --version 2>&1)"
fi

# ---------- 安装 Maven ----------
if ! command -v mvn &>/dev/null; then
    echo "[3/5] 安装 Maven..."
    if apt-cache show maven &>/dev/null 2>&1; then
        apt-get install -y maven
    else
        echo "  仓库中无 maven，手动安装 3.9.16..."
        MVN_URL="https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.tar.gz"
        curl -fsSL "$MVN_URL" -o /tmp/maven.tar.gz
        tar -xzf /tmp/maven.tar.gz -C /opt
        ln -sf /opt/apache-maven-3.9.16/bin/mvn /usr/local/bin/mvn
        rm -f /tmp/maven.tar.gz
    fi
else
    echo "[3/5] Maven 已安装: $(mvn --version 2>&1 | head -1)"
fi

# ---------- 拉取代码 ----------
if [ -d "$PROJECT_DIR" ]; then
    echo "[4/5] 更新已有仓库..."
    cd "$PROJECT_DIR"
    git pull
else
    echo "[4/5] 克隆仓库: $REPO_URL"
    mkdir -p "$WORK_DIR"
    cd "$WORK_DIR"
    git clone "$REPO_URL"
fi

# ---------- Maven 构建 + Docker 打包 ----------
echo "[5/5] Maven 构建 & Docker 打包..."

cd "$PROJECT_DIR/xechat-commons"
mvn clean install -DskipTests -q

cd "$PROJECT_DIR/xechat-server"
mvn clean package -DskipTests -q

echo "  构建 Docker 镜像: ${IMAGE_NAME}:${IMAGE_TAG}"
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

echo ""
echo "============================================"
echo "  构建完成!"
echo "  镜像: ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""
echo "  启动示例:"
echo "    docker run -d -p 1024:1024 -p 1025:1025 \\"
echo "      -v xechat-data:/app/data \\"
echo "      --name xechat ${IMAGE_NAME}:${IMAGE_TAG}"
echo "============================================"
