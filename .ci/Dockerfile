FROM registry.gitlab.com/reportix/ci-base-image:v1.0.3
LABEL maintainer="Federico Cavalieri <f@reportix.com>"

ENV NODE_VERSION 16.9.0

# Install Node
RUN curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64.tar.gz" && \
    curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt.asc" && \
    grep " node-v$NODE_VERSION-linux-x64.tar.gz\$" SHASUMS256.txt.asc | sha256sum -c - && \
    tar -xzf "node-v$NODE_VERSION-linux-x64.tar.gz" -C /usr/local --strip-components=1 && \
    rm "node-v$NODE_VERSION-linux-x64.tar.gz" SHASUMS256.txt.asc

#Intall Packages
RUN apt-get update && apt-get install openjdk-11-jdk -y
