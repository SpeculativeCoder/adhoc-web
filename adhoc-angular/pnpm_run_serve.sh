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

# TODO: need to see file changes

MSYS_NO_PATHCONV=1 \
docker build -t node_pnpm ./node_pnpm/. \
&& \
MSYS_NO_PATHCONV=1 \
docker run \
-v ./.angular:/home/node/app/.angular \
-v ./dist:/home/node/app/dist \
-v ./node_modules:/home/node/app/node_modules:ro \
-v ./src:/home/node/app/src:ro \
-v ./angular.json:/home/node/app/angular.json:ro \
-v ./package.json:/home/node/app/package.json:ro \
-v ./pnpm-workspace.yaml:/home/node/app/pnpm-workspace.yaml:ro \
-v ./tsconfig.app.json:/home/node/app/tsconfig.app.json:ro \
-v ./tsconfig.json:/home/node/app/tsconfig.json:ro \
-v ./tsconfig.spec.json:/home/node/app/tsconfig.spec.json:ro \
-p 127.0.0.1:4200:4200/tcp \
-i -t --rm \
node_pnpm \
pnpm run start --host 0.0.0.0
