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

export ADHOC_NAME=${ADHOC_NAME:-adhoc}

export AWS_PROFILE_FOR_ECR=${AWS_PROFILE_FOR_ECR:-adhoc_admin}

export MANAGER_IMAGE=${MANAGER_IMAGE:-${ADHOC_NAME}_dev_manager}
export KIOSK_IMAGE=${KIOSK_IMAGE:-${ADHOC_NAME}_dev_kiosk}
export SERVER_IMAGE=${SERVER_IMAGE:-${ADHOC_NAME}_dev_server}

export AWS_REGION=${AWS_REGION:-us-east-1}
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text --profile ${AWS_PROFILE_FOR_ECR})

docker tag ${MANAGER_IMAGE}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${MANAGER_IMAGE}:latest
docker tag ${KIOSK_IMAGE}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${KIOSK_IMAGE}:latest
docker tag ${SERVER_IMAGE}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${SERVER_IMAGE}:latest

aws ecr get-login-password --profile ${AWS_PROFILE_FOR_ECR} \
  | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${MANAGER_IMAGE}:latest
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${KIOSK_IMAGE}:latest
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${SERVER_IMAGE}:latest
