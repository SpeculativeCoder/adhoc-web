FROM ubuntu:rolling

RUN apt-get update -y && apt-get dist-upgrade -y
RUN apt-get install -y nodejs npm

ENV npm_config_ignore_scripts=true
ENV npm_config_engine_strict=true

ENV npm_config_minimum_release_age=2880
ENV npm_config_node_linker=hoisted

ENV npm_config_loglevel=verbose

RUN npm install --ignore-scripts -g pnpm@latest-10

RUN useradd -ms /bin/bash node

RUN mkdir /home/node/app
RUN chown -R node /home/node/app

WORKDIR /home/node/app

USER node

RUN pnpm config set minimum-release-age 2880

RUN pnpm --version

CMD ["pnpm", "--version"]
