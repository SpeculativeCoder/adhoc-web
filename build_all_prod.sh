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
source ./env/prod.env || true
source ./env/common.env || true
set +a

export ADHOC_ENV=prod

export ANGULAR_CONFIGURATION=${ANGULAR_CONFIGURATION:-production}
export CLIENT_UNREAL_CONFIGURATION=${CLIENT_UNREAL_CONFIGURATION:-Shipping}
export SERVER_UNREAL_CONFIGURATION=${SERVER_UNREAL_CONFIGURATION:-Shipping}
export FEATURE_FLAGS=${FEATURE_FLAGS:-production}

# use the real domain(s) for prod
export MANAGER_DOMAIN=${MANAGER_DOMAIN:-manager.${ADHOC_DOMAIN}}
export KIOSK_DOMAIN=${KIOSK_DOMAIN:-${ADHOC_DOMAIN}}
export SERVER_DOMAIN=${SERVER_DOMAIN:-server.${ADHOC_DOMAIN}}

./build_all.sh
