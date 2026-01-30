#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# Script gerado a partir de Apps.sh — execute como seu usuário (use sudo quando necessário)
# Uso: sudo bash install_manual.sh   (ou execute por etapas copiando os blocos)

export ANDROID_API=34
export BUILD_TOOLS=34.0.0
export NDK_VERSION=26.3.11579264
export CMAKE_VERSION=3.22.1
export ANDROID_SDK_DIR="$HOME/android-sdk"
export FLUTTER_DIR="${FLUTTER_DIR:-$HOME/flutter}"
export DOTNET_USER_DIR="$HOME/.dotnet"
export DEBIAN_FRONTEND=noninteractive

log(){ echo "[INFO] $*"; }
err(){ echo "[ERR] $*" >&2; }

log "Fase 1: atualizar apt e instalar pacotes essenciais"
sudo apt-get update -y
sudo apt-get install -y ca-certificates gnupg wget curl apt-transport-https software-properties-common \
  build-essential gcc g++ make cmake ninja-build pkg-config autoconf libtool clang llvm lld gdb \
  python3 python3-pip git unzip zip tar xz-utils libsdl2-dev libopenal-dev libfreetype6-dev libfontconfig1-dev \
  libssl-dev zlib1g-dev libglu1-mesa libgl1 libgtk-3-0 libnss3 libxss1 || log "Alguns pacotes podem ter falhado"

log "Fase 2: Node.js v20 (NodeSource)"
sudo mkdir -p /etc/apt/keyrings
sudo rm -f /etc/apt/keyrings/nodesource.gpg || true
curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | sudo gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" | sudo tee /etc/apt/sources.list.d/nodesource.list
sudo apt-get update -y
sudo apt-get install -y nodejs || log "Falha ao instalar nodejs"

log "Fase 3: Java 17"
sudo apt-get install -y openjdk-17-jdk || log "Falha ao instalar openjdk-17-jdk"
if command -v javac >/dev/null 2>&1; then
  javac_path=$(readlink -f "$(command -v javac)")
  java_home_dir=$(dirname "$(dirname "$javac_path")")
  echo "export JAVA_HOME=$java_home_dir" >> ~/.bashrc
  echo "export PATH=$java_home_dir/bin:\$PATH" >> ~/.bashrc
fi

log "Fase 4: .NET 8 (usuário)"
curl -fsSL https://dot.net/v1/dotnet-install.sh -o /tmp/dotnet-install.sh
chmod +x /tmp/dotnet-install.sh
mkdir -p "$DOTNET_USER_DIR"
bash /tmp/dotnet-install.sh --channel 8.0 --install-dir "$DOTNET_USER_DIR" || log ".NET install script retornou não-zero"
# adiciona variáveis ao profile
echo "export DOTNET_ROOT=$DOTNET_USER_DIR" >> ~/.bashrc
echo "export PATH=$DOTNET_USER_DIR:\$PATH" >> ~/.bashrc
echo "export PATH=$DOTNET_USER_DIR/tools:\$PATH" >> ~/.bashrc
source ~/.bashrc || true
# tenta instalar workload android (pode falhar sem internet ou sem manifest atualizado)
"$DOTNET_USER_DIR/dotnet" workload install android --skip-manifest-update || log "Falha ao instalar workload android"
rm -f /tmp/dotnet-install.sh

log "Fase 5: Android SDK (cmdline-tools + pacotes)"
mkdir -p "$ANDROID_SDK_DIR/cmdline-tools" "$ANDROID_SDK_DIR/platform-tools" "$HOME/.android"
touch "$HOME/.android/repositories.cfg"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
curl -fsSL "$CMDLINE_TOOLS_URL" -o /tmp/cmdline-tools.zip
unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-extract || true
mkdir -p "$ANDROID_SDK_DIR/cmdline-tools/latest"
cp -r /tmp/cmdline-extract/cmdline-tools/* "$ANDROID_SDK_DIR/cmdline-tools/latest/" 2>/dev/null || \
cp -r /tmp/cmdline-extract/tools/* "$ANDROID_SDK_DIR/cmdline-tools/latest/" 2>/dev/null || \
cp -r /tmp/cmdline-extract/* "$ANDROID_SDK_DIR/cmdline-tools/latest/" 2>/dev/null || true
rm -rf /tmp/cmdline-extract /tmp/cmdline-tools.zip

echo "export ANDROID_HOME=$ANDROID_SDK_DIR" >> ~/.bashrc
echo "export ANDROID_SDK_ROOT=$ANDROID_SDK_DIR" >> ~/.bashrc
echo "export PATH=$ANDROID_SDK_DIR/cmdline-tools/latest/bin:$ANDROID_SDK_DIR/platform-tools:\$PATH" >> ~/.bashrc
source ~/.bashrc || true
mkdir -p "$ANDROID_SDK_DIR/licenses"
# licenças comuns
cat > "$ANDROID_SDK_DIR/licenses/android-sdk-license" <<'L'
24333f8a63b6825ea9c5514f83c2829b004d1fee
84831b9409646a918e30573bab4c9c91346d8abd
L
echo "601085b94cd77f0b54ff86406957099ebe79c4d6" > "$ANDROID_SDK_DIR/licenses/android-googletv-license" || true
echo "33b6a2b64607f11b759f320ef9dff4ae5c47d97a" > "$ANDROID_SDK_DIR/licenses/google-gdk-license" || true
echo "e9acab5b5fbb560a72cfaecce6eb5b36b1e850d9" > "$ANDROID_SDK_DIR/licenses/mips-android-sysimage-license" || true

# instalar pacotes via sdkmanager (pode demorar)
if [ -x "$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
  yes | "$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --install \
    "platform-tools" \
    "platforms;android-$ANDROID_API" \
    "build-tools;$BUILD_TOOLS" \
    "ndk;$NDK_VERSION" \
    "cmake;$CMAKE_VERSION" || log "sdkmanager retornou não-zero"
else
  log "sdkmanager não encontrado em $ANDROID_SDK_DIR/cmdline-tools/latest/bin"
fi

log "Fase 6: Flutter (stable)"
if [ ! -x "$FLUTTER_DIR/bin/flutter" ]; then
  git clone https://github.com/flutter/flutter.git -b stable --depth 1 "$FLUTTER_DIR" || log "Falha ao clonar Flutter"
fi
echo "export FLUTTER_HOME=$FLUTTER_DIR" >> ~/.bashrc
echo "export PATH=$FLUTTER_DIR/bin:\$PATH" >> ~/.bashrc
export ANDROID_SDK_ROOT="$ANDROID_SDK_DIR"
export PATH="$ANDROID_SDK_DIR/platform-tools:$ANDROID_SDK_DIR/cmdline-tools/latest/bin:$PATH"
source ~/.bashrc || true
# aceitar licenças Android (não interativo)
yes | "$FLUTTER_DIR/bin/flutter" doctor --android-licenses || log "Falha ao aceitar licenças Android via flutter"

log "Fase 7: Mono e MonoGame"
sudo apt-get install -y mono-complete || log "Falha ao instalar mono"
"$DOTNET_USER_DIR/dotnet" new install MonoGame.Templates.CSharp || log "Falha ao instalar templates MonoGame"
"$DOTNET_USER_DIR/dotnet" tool install --global dotnet-mgcb-editor || log "Falha ao instalar dotnet-mgcb-editor"

log "Fase 8: Docker"
curl -fsSL https://get.docker.com -o /tmp/get-docker.sh
sudo sh /tmp/get-docker.sh || log "Falha ao instalar Docker"
rm -f /tmp/get-docker.sh
sudo usermod -aG docker "$USER" || true

log "Fase 9: VS Code (instalação)"
sudo apt-get install -y apt-transport-https wget gpg || true
wget -qO- https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor | sudo tee /etc/apt/keyrings/packages.microsoft.gpg >/dev/null || true
echo "deb [arch=amd64,arm64,armhf signed-by=/etc/apt/keyrings/packages.microsoft.gpg] https://packages.microsoft.com/repos/code stable main" | sudo tee /etc/apt/sources.list.d/vscode.list
sudo apt-get update -y
sudo apt-get install -y code || log "Falha ao instalar code"

log "Fase 10: Limpeza e verificação"
sudo apt-get clean || true
# verificações rápidas
log "Verificações:"
java -version || true
node -v || true
npm -v || true
"$DOTNET_USER_DIR/dotnet" --version || true
mono --version | head -n1 || true
"$FLUTTER_DIR/bin/flutter" --version || true
"$ANDROID_SDK_DIR/platform-tools/adb" version || true
docker --version || true
code --version | head -n1 || true

log "Script concluído. Reabra o terminal ou rode 'source ~/.bashrc' para carregar variáveis."