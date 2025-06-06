#!/bin/bash

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

set -x # show all commands being run
set -u # error on undefined variables
set -e # bail on ANY error

set -a
source ./env/common.env || true
source ./env/prod.env || true
set +a

export ADHOC_NAME=${ADHOC_NAME:-adhoc}

export SSL_ENABLED=${SSL_ENABLED:-false}

export ANGULAR_CONFIGURATION=${ANGULAR_CONFIGURATION:-production}
export CLIENT_UNREAL_CONFIGURATION=${CLIENT_UNREAL_CONFIGURATION:-Shipping}
export SERVER_UNREAL_CONFIGURATION=${SERVER_UNREAL_CONFIGURATION:-Shipping}
export FEATURE_FLAGS=${FEATURE_FLAGS:-production}

export POSTGRES_HOST=${POSTGRES_HOST:-${ADHOC_NAME}-prod-manager.${ADHOC_NAME}-prod}
export H2_HOST=${H2_HOST:-${ADHOC_NAME}-prod-manager.${ADHOC_NAME}-prod}
export HSQLDB_HOST=${HSQLDB_HOST:-${ADHOC_NAME}-prod-manager.${ADHOC_NAME}-prod}
export MANAGER_HOST=${MANAGER_HOST:-${ADHOC_NAME}-prod-manager.${ADHOC_NAME}-prod}
export KIOSK_HOST=${KIOSK_HOST:-${ADHOC_NAME}-prod-kiosk.${ADHOC_NAME}-prod}

export AWS_REGION=${AWS_REGION:-us-east-1}
export SERVER_AVAILABILITY_ZONE=${SERVER_AVAILABILITY_ZONE:-us-east-1a}
export SERVER_SECURITY_GROUP_NAME=${SERVER_SECURITY_GROUP_NAME:-${ADHOC_NAME}_prod_server}

export ECS_CLUSTER=${ECS_CLUSTER:-${ADHOC_NAME}_prod}
export ADHOC_DOMAIN=${ADHOC_DOMAIN:-localhost}
export ROUTE53_ZONE=${ROUTE53_ZONE:-localhost}
export MANAGER_DOMAIN=${MANAGER_DOMAIN:-manager.${ADHOC_DOMAIN}}
export KIOSK_DOMAIN=${KIOSK_DOMAIN:-${ADHOC_DOMAIN}}
export SERVER_DOMAIN=${SERVER_DOMAIN:-server.${ADHOC_DOMAIN}}

export UNREAL_PROJECT_NAME=${UNREAL_PROJECT_NAME:-MyProject}

export MANAGER_IMAGE=${MANAGER_IMAGE:-${ADHOC_NAME}_prod_manager}
export KIOSK_IMAGE=${KIOSK_IMAGE:-${ADHOC_NAME}_prod_kiosk}
export SERVER_IMAGE=${SERVER_IMAGE:-${ADHOC_NAME}_prod_server}

export MANAGER_SPRING_PROFILES_ACTIVE=${MANAGER_SPRING_PROFILES_ACTIVE:-db-h2postgres,hosting-ecs,dns-route53}
export KIOSK_SPRING_PROFILES_ACTIVE=${KIOSK_SPRING_PROFILES_ACTIVE:-db-h2postgres}

./build_all.sh
