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

set -u # error on undefined variables
set -e # bail on ANY error

# NOTE: this script is intended to be ran within a docker container and will copy certs from environment variables into files

[[ -v ADHOC_NAME ]] || echo "missing ADHOC_NAME"

[[ -v CA_CERTIFICATE ]] || echo "missing CA_CERTIFICATE"
[[ -v SERVER_CERTIFICATE ]] || echo "missing SERVER_CERTIFICATE"
[[ -v PRIVATE_KEY ]] || echo "missing PRIVATE_KEY"

# TODO
echo -e "${CA_CERTIFICATE}" > certs/${ADHOC_NAME}-ca.cer
echo -e "${SERVER_CERTIFICATE}" > certs/${ADHOC_NAME}.cer
echo -e "${PRIVATE_KEY}" > certs/${ADHOC_NAME}.key

ls -al certs

unset CA_CERTIFICATE
unset SERVER_CERTIFICATE
unset PRIVATE_KEY
