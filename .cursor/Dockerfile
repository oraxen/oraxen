# Use the official Ubuntu image as a base
FROM ubuntu:latest

# Avoid interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive

# Install OpenJDK 21, git, curl, and other common utilities
RUN apt-get update && \
    apt-get install -y \
    openjdk-21-jdk \
    curl \
    git \
    jq \
    unzip \
    zip \
    ca-certificates \
    sed && \
    rm -rf /var/lib/apt/lists/*

# Configure Java environment
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# (Optional) Install dotenvx system-wide for environment management
RUN curl -sfS https://dotenvx.sh/install.sh | sh

# Create a non-root user 'appuser' with a home directory
RUN useradd --create-home --shell /bin/bash appuser

# Switch to the new user before creating user-specific files
USER appuser
WORKDIR /home/appuser

# --- Minecraft Server Setup ---
# Create a directory for the Minecraft server
RUN mkdir /home/appuser/minecraft

# Download the latest Paper server jar (1.21.8, build #35)
RUN curl -o /home/appuser/minecraft/paper.jar "https://fill-data.papermc.io/v1/objects/bfbfb67f06dceaceebc8a390766f87f5c87c5b57094244b25334b8a2a99d0d73/paper-1.21.8-35.jar"

# Create the plugins directory for the server
RUN mkdir /home/appuser/minecraft/plugins

# Download the latest ProtocolLib jar (v5.4.0) into the plugins directory
RUN curl -L -o /home/appuser/minecraft/plugins/ProtocolLib.jar "https://github.com/dmulloy2/ProtocolLib/releases/download/5.4.0/ProtocolLib.jar"

# Agree to the Minecraft EULA to allow the server to start
RUN echo "eula=true" > /home/appuser/minecraft/eula.txt
# --- End Minecraft Server Setup ---

# Set the final working directory for the development environment.
# A project repository will typically be cloned here.
WORKDIR /home/appuser

# Set the default command to open a shell for development.
CMD ["bash"]