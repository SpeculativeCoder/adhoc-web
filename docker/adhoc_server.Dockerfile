#
# Copyright (c) 2022-2025 SpeculativeCoder (https://github.com/SpeculativeCoder)
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

FROM ubuntu:rolling

RUN apt-get update -y && apt-get dist-upgrade -y
# RUN apt-get install -y openssl ca-certificates

ARG ADHOC_NAME=adhoc

ARG UNREAL_SERVER_CONFIGURATION=Development

ARG SSL_ENABLED=false
ARG FEATURE_FLAGS=development
ARG MANAGER_HOST=host.docker.internal
ARG UNREAL_PROJECT_NAME=MyProject
ARG SERVER_BASIC_AUTH_PASSWORD

ENV ADHOC_NAME=${ADHOC_NAME}
ENV SSL_ENABLED=${SSL_ENABLED}
ENV FEATURE_FLAGS=${FEATURE_FLAGS}
ENV MANAGER_HOST=${MANAGER_HOST}
ENV UNREAL_PROJECT_NAME=${UNREAL_PROJECT_NAME}
ENV SERVER_BASIC_AUTH_PASSWORD=${SERVER_BASIC_AUTH_PASSWORD}
ENV MAP_NAME=Region0001
ENV SERVER_ID=1
ENV REGION_ID=1
ENV INITIAL_AREA_INDEXES=0
ENV MAX_PAWNS=8
ENV MAX_PLAYERS=8
ENV MAX_BOTS=8

EXPOSE 8889

RUN useradd -ms /bin/bash adhoc

RUN mkdir LinuxServer
WORKDIR LinuxServer

RUN mkdir certs
#ADD certs/adhoc-ca.cer certs/adhoc-ca.cer
#ADD certs/adhoc.cer certs/adhoc.cer
#ADD certs/adhoc.key certs/adhoc.key
ADD docker/adhoc_container_init.sh .
RUN chmod +x adhoc_container_init.sh

ADD Package/${UNREAL_SERVER_CONFIGURATION}/LinuxServer/ /LinuxServer/
RUN chown -R adhoc /LinuxServer

USER adhoc

ENTRYPOINT ./adhoc_container_init.sh && cat /etc/hosts && ./${UNREAL_PROJECT_NAME}Server.sh ${MAP_NAME}?MaxPlayers=${MAX_PLAYERS} MaxControllers=${MAX_CONTROLLERS} MaxBots=${MAX_BOTS} ServerID=${SERVER_ID} RegionID=${REGION_ID} InitialAreaIndexes=${INITIAL_AREA_INDEXES} PrivateIP=$(cat /etc/hosts | tail -1 | awk {'print $1'}) ManagerHost=${MANAGER_HOST} FeatureFlags=${FEATURE_FLAGS} FORCELOGFLUSH=1 -ini:Engine:[/Script/WebSocketNetworking.WebSocketNetworkingSettings]:bEnableSSL=${SSL_ENABLED}
