FROM mcr.microsoft.com/devcontainers/java:1-17-bookworm

# Variables de entorno para Android SDK
ENV ANDROID_SDK_ROOT=/usr/local/android-sdk
ENV ANDROID_HOME=${ANDROID_SDK_ROOT}
ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

# Remover repositorio Yarn desactualizado de la imagen base y actualizar paquetes
RUN rm -f /etc/apt/sources.list.d/yarn.list \
    && apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    curl \
    git \
    usbutils \
    && curl -fsSL https://d2lang.com/install.sh | sh -s -- \
    && rm -rf /var/lib/apt/lists/*

# Instalar Android Command Line Tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

# Aceptar licencias e instalar SDK de Android (API 35 y Build-Tools 34 y 35)
RUN yes | sdkmanager --licenses > /dev/null \
    && sdkmanager "platforms;android-35" "build-tools;35.0.0" "build-tools;34.0.0" "platform-tools" \
    && chown -R vscode:vscode ${ANDROID_SDK_ROOT}
