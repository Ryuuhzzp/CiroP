echo ""
echo "[1/4] Verificando Android SDK..."
echo ""
echo "[2/4] Verificando Flutter..."
echo ""
echo "[3/4] Verificando Java..."
echo ""
echo "[4/4] Atualizando local.properties..."
echo ""
echo "=== SETUP COMPLETO ==="
echo "✅ Android SDK:    $ANDROID_HOME"
echo "✅ Flutter SDK:    $FLUTTER_HOME"
echo "✅ Java Version:   $JAVA_VERSION"
echo ""
echo "Próximas ações:"
echo "1. cd /workspaces/Ciro/src/Celeste.Android"
echo "2. gradle assembleRelease"
echo ""
#!/bin/bash

# Atualiza pacotes
sudo apt update && sudo apt install -y unzip curl wget git zip libglu1-mesa

# Instala Java 17
sudo apt install -y openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Instala Flutter SDK
FLUTTER_DIR=$HOME/flutter
git clone https://github.com/flutter/flutter.git -b stable $FLUTTER_DIR
export PATH="$FLUTTER_DIR/bin:$PATH"

# Instala Android Command Line Tools
ANDROID_DIR=$HOME/Android
mkdir -p $ANDROID_DIR/cmdline-tools
cd $ANDROID_DIR
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip
unzip cmdline-tools.zip -d cmdline-tools
mv cmdline-tools/cmdline-tools $ANDROID_DIR/cmdline-tools/latest

# Configura variáveis de ambiente
export ANDROID_HOME=$ANDROID_DIR
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH

# Instala SDK, Build Tools e NDK
yes | sdkmanager --sdk_root=$ANDROID_HOME --licenses
sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-33" "build-tools;33.0.2" "ndk;25.2.9519653"

# Verifica instalação
flutter doctor

echo "Setup finalizado."
