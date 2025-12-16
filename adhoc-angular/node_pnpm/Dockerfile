FROM node:lts-trixie

ENV npm_config_ignore_scripts=true
ENV npm_config_engine_strict=true

ENV npm_config_minimum_release_age=2880
ENV npm_config_node_linker=hoisted

ENV npm_config_loglevel=verbose

RUN npm install --ignore-scripts -g pnpm@latest-10

RUN mkdir /home/node/app
RUN chown -R node:node /home/node/app

WORKDIR /home/node/app

USER node

RUN pnpm --version
RUN pnpm config list

CMD ["pnpm", "--version"]
