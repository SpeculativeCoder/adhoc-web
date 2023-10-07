#!/bin/bash

#
# Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

set -x # show all commands being run
set -u # error on undefined variables
set -e # bail on ANY error

export ADHOC_NAME=${ADHOC_NAME:-adhoc}

export SSL_ENABLED=${SSL_ENABLED:-false}

export ANGULAR_CONFIGURATION=${ANGULAR_CONFIGURATION:-development}
export CLIENT_UNREAL_CONFIGURATION=${CLIENT_UNREAL_CONFIGURATION:-Development}
export FEATURE_FLAGS=${FEATURE_FLAGS:-development}

export POSTGRES_HOST=${POSTGRES_HOST:-${ADHOC_NAME}-dev-manager.${ADHOC_NAME}-dev}
export HSQLDB_HOST=${HSQLDB_HOST:-${ADHOC_NAME}-dev-manager.${ADHOC_NAME}-dev}
export MANAGER_HOST=${MANAGER_HOST:-${ADHOC_NAME}-dev-manager.${ADHOC_NAME}-dev}
export KIOSK_HOST=${KIOSK_HOST:-${ADHOC_NAME}-dev-kiosk.${ADHOC_NAME}-dev}

export AWS_REGION=${AWS_REGION:-us-east-1}
export SERVER_AVAILABILITY_ZONE=${SERVER_AVAILABILITY_ZONE:-us-east-1a}
export SERVER_SECURITY_GROUP_NAME=${SERVER_SECURITY_GROUP_NAME:-${ADHOC_NAME}_dev_server}

export ECS_CLUSTER=${ECS_CLUSTER:-${ADHOC_NAME}_dev}
export ADHOC_DOMAIN=${ADHOC_DOMAIN:-localhost}
export MANAGER_DOMAIN=${MANAGER_DOMAIN:-manager-dev.${ADHOC_DOMAIN}}
export KIOSK_DOMAIN=${KIOSK_DOMAIN:-dev.${ADHOC_DOMAIN}}
export SERVER_DOMAIN=${SERVER_DOMAIN:-server-dev.${ADHOC_DOMAIN}}

export UNREAL_PROJECT_NAME=${UNREAL_PROJECT_NAME:-MyProject}
export UNREAL_PROJECT_REGION_MAPS=${UNREAL_PROJECT_REGION_MAPS:-Region0001}

export MANAGER_IMAGE=${MANAGER_IMAGE:-${ADHOC_NAME}_dev_manager}
export KIOSK_IMAGE=${KIOSK_IMAGE:-${ADHOC_NAME}_dev_kiosk}
export SERVER_IMAGE=${SERVER_IMAGE:-${ADHOC_NAME}_dev_server}

mvn clean package -DskipTests -Dangular.configuration=${ANGULAR_CONFIGURATION},customization -Dunreal.configuration=${CLIENT_UNREAL_CONFIGURATION}

docker build --tag ${MANAGER_IMAGE} -f docker/adhoc_manager.Dockerfile \
  --build-arg ADHOC_NAME=${ADHOC_NAME} \
  --build-arg SSL_ENABLED=${SSL_ENABLED} \
  --build-arg FEATURE_FLAGS=${FEATURE_FLAGS} \
  --build-arg POSTGRES_HOST=${POSTGRES_HOST} \
  --build-arg HSQLDB_HOST=${HSQLDB_HOST} \
  --build-arg MANAGER_HOST=${MANAGER_HOST} \
  --build-arg KIOSK_HOST=${KIOSK_HOST} \
  --build-arg AWS_REGION=${AWS_REGION} \
  --build-arg SERVER_AVAILABILITY_ZONE=${SERVER_AVAILABILITY_ZONE} \
  --build-arg SERVER_SECURITY_GROUP_NAME=${SERVER_SECURITY_GROUP_NAME} \
  --build-arg ECS_CLUSTER=${ECS_CLUSTER} \
  --build-arg ADHOC_DOMAIN=${ADHOC_DOMAIN} \
  --build-arg MANAGER_DOMAIN=${MANAGER_DOMAIN} \
  --build-arg KIOSK_DOMAIN=${KIOSK_DOMAIN} \
  --build-arg SERVER_DOMAIN=${SERVER_DOMAIN} \
  --build-arg UNREAL_PROJECT_NAME=${UNREAL_PROJECT_NAME} \
  --build-arg UNREAL_PROJECT_REGION_MAPS=${UNREAL_PROJECT_REGION_MAPS} \
  --build-arg MANAGER_IMAGE=${MANAGER_IMAGE} \
  --build-arg KIOSK_IMAGE=${KIOSK_IMAGE} \
  --build-arg SERVER_IMAGE=${SERVER_IMAGE} \
  .

docker build --tag ${KIOSK_IMAGE} -f docker/adhoc_kiosk.Dockerfile \
  --build-arg ADHOC_NAME=${ADHOC_NAME} \
  --build-arg SSL_ENABLED=${SSL_ENABLED} \
  --build-arg FEATURE_FLAGS=${FEATURE_FLAGS} \
  --build-arg POSTGRES_HOST=${POSTGRES_HOST} \
  --build-arg HSQLDB_HOST=${HSQLDB_HOST} \
  --build-arg MANAGER_HOST=${MANAGER_HOST} \
  --build-arg KIOSK_HOST=${KIOSK_HOST} \
  --build-arg AWS_REGION=${AWS_REGION} \
  --build-arg SERVER_AVAILABILITY_ZONE=${SERVER_AVAILABILITY_ZONE} \
  --build-arg SERVER_SECURITY_GROUP_NAME=${SERVER_SECURITY_GROUP_NAME} \
  --build-arg ECS_CLUSTER=${ECS_CLUSTER} \
  --build-arg ADHOC_DOMAIN=${ADHOC_DOMAIN} \
  --build-arg MANAGER_DOMAIN=${MANAGER_DOMAIN} \
  --build-arg KIOSK_DOMAIN=${KIOSK_DOMAIN} \
  --build-arg SERVER_DOMAIN=${SERVER_DOMAIN} \
  --build-arg UNREAL_PROJECT_NAME=${UNREAL_PROJECT_NAME} \
  --build-arg UNREAL_PROJECT_REGION_MAPS=${UNREAL_PROJECT_REGION_MAPS} \
  --build-arg MANAGER_IMAGE=${MANAGER_IMAGE} \
  --build-arg KIOSK_IMAGE=${KIOSK_IMAGE} \
  --build-arg SERVER_IMAGE=${SERVER_IMAGE} \
  .
