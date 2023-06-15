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

export SERVER_UNREAL_CONFIGURATION=${SERVER_UNREAL_CONFIGURATION:-Development}
export FEATURE_FLAGS=${FEATURE_FLAGS:-development}

export MANAGER_HOST=${MANAGER_HOST:-adhoc-dev-manager.adhoc-dev}

export UNREAL_PROJECT_NAME=${UNREAL_PROJECT_NAME:-MyProject}
# TODO: better default due to risk of long path?
export UNREAL_PROJECT_DIR=${UNREAL_PROJECT_DIR:-${HOME}/Unreal\ Projects/${UNREAL_PROJECT_NAME}}
export UNREAL_ENGINE_DIR=${UNREAL_ENGINE_DIR:-${HOME}/ue-4.27-html5-es3}

export SERVER_IMAGE=${SERVER_IMAGE:-adhoc_dev_server}

package_dir=$(realpath -e $(dirname "$0")/Package)

mkdir -p ${package_dir}/${SERVER_UNREAL_CONFIGURATION}

${UNREAL_ENGINE_DIR}/Engine/Build/BatchFiles/RunUAT.bat BuildCookRun \
 -Project=${UNREAL_PROJECT_DIR}/${UNREAL_PROJECT_NAME}.uproject \
 -Target=${UNREAL_PROJECT_NAME}Server -TargetPlatform=Linux -ServerConfig=${SERVER_UNREAL_CONFIGURATION} \
 -Build -Cook -Stage -Package -Archive \
 -MapsToCook=Transition+Region0001 -Map=Transition \
 -ArchiveDirectory=${package_dir}/${SERVER_UNREAL_CONFIGURATION} \
 -PreReqs -Pak -Compressed -NoCompileEditor -SkipCookingEditorContent \
 -NoP4 -UTF8Output -NoDebugInfo

docker build --tag ${SERVER_IMAGE} -f docker/adhoc_server.Dockerfile \
  --build-arg SERVER_UNREAL_CONFIGURATION=${SERVER_UNREAL_CONFIGURATION} \
  --build-arg FEATURE_FLAGS=${FEATURE_FLAGS} \
  --build-arg MANAGER_HOST=${MANAGER_HOST} \
  .