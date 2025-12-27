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
set +a

export ADHOC_ENV=${ADHOC_ENV:-dev}

export ADHOC_NAME=${ADHOC_NAME:-adhoc}

export UNREAL_SERVER_CONFIGURATION=${UNREAL_SERVER_CONFIGURATION:-Development}
export SSL_ENABLED=${SSL_ENABLED:-false}
export FEATURE_FLAGS=${FEATURE_FLAGS:-development}

export MANAGER_HOST=${MANAGER_HOST:-${ADHOC_NAME}-${ADHOC_ENV}-manager.${ADHOC_NAME}-${ADHOC_ENV}}

export UNREAL_PROJECT_NAME=${UNREAL_PROJECT_NAME:-MyProject}
export UNREAL_PROJECT_REGION_MAPS=${UNREAL_PROJECT_REGION_MAPS:-Region0001}
export UNREAL_PROJECT_TRANSITION_MAP=${UNREAL_PROJECT_TRANSITION_MAP:-Entry}
# TODO: better default due to risk of long path?
export UNREAL_PROJECT_DIR=${UNREAL_PROJECT_DIR:-${HOME}/Unreal\ Projects/${UNREAL_PROJECT_NAME}}
export UNREAL_ENGINE_DIR=${UNREAL_ENGINE_DIR:-${HOME}/ue-4.27-html5-es3}

export SERVER_IMAGE=${SERVER_IMAGE:-${ADHOC_NAME}_${ADHOC_ENV}_server}

web_project_dir=$(realpath -e $(dirname "$0"))
package_dir=${web_project_dir}/Package

mkdir -p ${package_dir}/${UNREAL_SERVER_CONFIGURATION}

# TODO: array join on maps
${UNREAL_ENGINE_DIR}/Engine/Build/BatchFiles/RunUAT.bat BuildCookRun \
 -Project=${UNREAL_PROJECT_DIR}/${UNREAL_PROJECT_NAME}.uproject \
 -Target=${UNREAL_PROJECT_NAME}Server -TargetPlatform=Linux -ServerConfig=${UNREAL_SERVER_CONFIGURATION} \
 -Build -Cook -Stage -Package -Archive \
 -MapsToCook=${UNREAL_PROJECT_REGION_MAPS}+${UNREAL_PROJECT_TRANSITION_MAP} -Map=${UNREAL_PROJECT_TRANSITION_MAP} \
 -ArchiveDirectory=${package_dir}/${UNREAL_SERVER_CONFIGURATION} \
 -PreReqs -Pak -Compressed -NoCompileEditor -SkipCookingEditorContent \
 -NoP4 -UTF8Output -NoDebugInfo

# we only want the symbols for now so delete debug info
#debug_filepath=${package_dir}/${SERVER_UNREAL_CONFIGURATION}/LinuxServer/${UNREAL_PROJECT_NAME}/Binaries/Linux/${UNREAL_PROJECT_NAME}Server.debug
#rm -v $debug_filepath

docker build --tag ${SERVER_IMAGE} --tag adhoc_${ADHOC_ENV}_server -f docker/adhoc_server.Dockerfile \
  --build-arg ADHOC_NAME=${ADHOC_NAME} \
  --build-arg UNREAL_SERVER_CONFIGURATION=${UNREAL_SERVER_CONFIGURATION} \
  --build-arg SSL_ENABLED=${SSL_ENABLED} \
  --build-arg FEATURE_FLAGS=${FEATURE_FLAGS} \
  --build-arg MANAGER_HOST=${MANAGER_HOST} \
  --build-arg UNREAL_PROJECT_NAME=${UNREAL_PROJECT_NAME} \
  .
