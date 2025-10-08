FROM node:trixie

RUN corepack enable pnpm

USER node

RUN mkdir /home/node/app

WORKDIR /home/node/app

ENV npm_config_ignore_scripts=true
ENV npm_config_engine_strict=true

ENV npm_config_minimum_release_age=2880
ENV npm_config_node_linker=hoisted

ENV npm_config_loglevel=info

RUN pnpm --version

CMD ["pnpm", "--version"]
