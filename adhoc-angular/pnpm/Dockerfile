FROM node:trixie

RUN corepack enable pnpm

USER node

RUN mkdir /home/node/app

WORKDIR /home/node/app

ENV npm_config_ignore_scripts=true
ENV npm_config_engine_strict=true
ENV npm_config_loglevel=info

ENV npm_config_minimum_release_age=2880

RUN pnpm --version

CMD ["pnpm", "--version"]
