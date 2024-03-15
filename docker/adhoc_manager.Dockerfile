#
# Copyright (c) 2022-2024 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
RUN apt-get install -y openjdk-21-jre-headless curl
#ca-certificates

ARG ADHOC_NAME=adhoc

ARG SSL_ENABLED=false

ARG FEATURE_FLAGS=development

ARG POSTGRES_HOST=host.docker.internal
ARG HSQLDB_HOST=host.docker.internal
ARG MANAGER_HOST=host.docker.internal
ARG KIOSK_HOST=host.docker.internal

ARG AWS_REGION=us-east-1
ARG SERVER_AVAILABILITY_ZONE=us-east-1a
ARG SERVER_SECURITY_GROUP_NAME=adhoc_dev_server

ARG ECS_CLUSTER=adhoc_dev
ARG ADHOC_DOMAIN=localhost
ARG ROUTE53_ZONE=localhost
ARG MANAGER_DOMAIN=manager-dev.localhost
ARG SERVER_DOMAIN=server-dev.localhost
ARG KIOSK_DOMAIN=dev.localhost

ARG UNREAL_PROJECT_NAME=MyProject
ARG UNREAL_PROJECT_REGION_MAPS=Region0001

ARG MANAGER_IMAGE=adhoc_dev_manager
ARG KIOSK_IMAGE=adhoc_dev_kiosk
ARG SERVER_IMAGE=adhoc_dev_server

#ARG SPRING_PROFILES_ACTIVE=db-postgres,hosting-ecs,dns-route53
ARG SPRING_PROFILES_ACTIVE=db-hsqldb,hosting-ecs,dns-route53

ENV ADHOC_NAME=${ADHOC_NAME}

ENV SSL_ENABLED=${SSL_ENABLED}

ENV FEATURE_FLAGS=${FEATURE_FLAGS}

ENV POSTGRES_HOST=${POSTGRES_HOST}
ENV HSQLDB_HOST=${HSQLDB_HOST}
ENV MANAGER_HOST=${MANAGER_HOST}
ENV KIOSK_HOST=${KIOSK_HOST}

ENV AWS_REGION=${AWS_REGION}
ENV SERVER_AVAILABILITY_ZONE=${SERVER_AVAILABILITY_ZONE}
ENV SERVER_SECURITY_GROUP_NAME=${SERVER_SECURITY_GROUP_NAME}

ENV ECS_CLUSTER=${ECS_CLUSTER}
ENV ADHOC_DOMAIN=${ADHOC_DOMAIN}
ENV ROUTE53_ZONE=${ROUTE53_ZONE}
ENV MANAGER_DOMAIN=${MANAGER_DOMAIN}
ENV KIOSK_DOMAIN=${KIOSK_DOMAIN}
ENV SERVER_DOMAIN=${SERVER_DOMAIN}

ENV UNREAL_PROJECT_NAME=${UNREAL_PROJECT_NAME}
ENV UNREAL_PROJECT_REGION_MAPS=${UNREAL_PROJECT_REGION_MAPS}

ENV MANAGER_IMAGE=${MANAGER_IMAGE}
ENV KIOSK_IMAGE=${KIOSK_IMAGE}
ENV SERVER_IMAGE=${SERVER_IMAGE}

ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

ENV SERVER_PORT=443
ENV SERVER_PORT_2=80

EXPOSE 443 80

#RUN useradd -ms /bin/bash adhoc

RUN mkdir certs
#ADD certs/adhoc-ca.cer certs/adhoc-ca.cer
#ADD certs/adhoc.cer certs/adhoc.cer
#ADD certs/adhoc.key certs/adhoc.key
ADD docker/adhoc_container_init.sh .
RUN chmod +x adhoc_container_init.sh

ADD adhoc-manager/target/adhoc-manager-0.0.1-SNAPSHOT.jar adhoc-manager.jar

#USER adhoc

ENTRYPOINT ../adhoc_container_init.sh && cat /etc/hosts && echo ${SPRING_PROFILES_ACTIVE} && java -DMESSAGE_BROKER_HOST=$(cat /etc/hosts | tail -1 | awk {'print $1'}) -jar adhoc-manager.jar
