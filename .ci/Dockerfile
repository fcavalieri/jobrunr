FROM ubuntu:20.04

LABEL maintainer="Federico Cavalieri <f@reportix.com>"

ENV KEYSERVER hkp://keyserver.ubuntu.com:80
ENV TZ=Europe/Zurich
ENV NODE_VERSION 12.20.1
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Add repositories for dependencies
RUN apt-get update && \
    apt-get install curl wget apt-transport-https gnupg -y && \
    apt-key adv --keyserver "$KEYSERVER" --recv-keys DA1A4A13543B466853BAF164EB9B1D8886F44E2A && \
    echo "deb http://ppa.launchpad.net/openjdk-r/ppa/ubuntu focal main " | tee /etc/apt/sources.list.d/openjdk.list && \
    apt-key adv --keyserver "$KEYSERVER" --recv-keys 7EA0A9C3F273FCD8 && \
    echo "deb [arch=amd64] https://download.docker.com/linux/ubuntu focal stable" | tee /etc/apt/sources.list.d/docker.list && \
    apt-get update

RUN apt-get install -y locales && \
    locale-gen "en_US.UTF-8"
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

# Install Node
RUN curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64.tar.gz" && \
    curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt.asc" && \
    grep " node-v$NODE_VERSION-linux-x64.tar.gz\$" SHASUMS256.txt.asc | sha256sum -c - && \
    tar -xzf "node-v$NODE_VERSION-linux-x64.tar.gz" -C /usr/local --strip-components=1 && \
    rm "node-v$NODE_VERSION-linux-x64.tar.gz" SHASUMS256.txt.asc

#Intall Packages
RUN apt-get install openjdk-11-jdk docker-ce rsync \
                    maven git nano iproute2 net-tools iputils-ping unzip -y
