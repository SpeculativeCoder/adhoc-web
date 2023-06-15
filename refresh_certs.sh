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

set -a
source ./env/common.env || true
set +a

export AWS_PROFILE_FOR_ROUTE53=${AWS_PROFILE_FOR_ROUTE53:-adhoc_acme}

export ADHOC_DOMAIN=${ADHOC_DOMAIN:-localhost}

set +x
export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id --profile ${AWS_PROFILE_FOR_ROUTE53})
export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key --profile ${AWS_PROFILE_FOR_ROUTE53})
set -x

mkdir -p certs

~/.acme.sh/acme.sh --issue --force --dns dns_aws -d ${ADHOC_DOMAIN} -d *.${ADHOC_DOMAIN}

# put the public key first in the private key file (not sure why this is needed)
cat "$HOME/.acme.sh/${ADHOC_DOMAIN}/${ADHOC_DOMAIN}.cer" > certs/adhoc.key
# follow this with the actual private key
cat "$HOME/.acme.sh/${ADHOC_DOMAIN}/${ADHOC_DOMAIN}.key" >> certs/adhoc.key

# put the "full chain" in the public key file
cat "$HOME/.acme.sh/${ADHOC_DOMAIN}/fullchain.cer" > certs/adhoc.cer

# root certificate
# TODO
#/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem
