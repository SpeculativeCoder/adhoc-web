FROM node:lts-trixie

ENV npm_config_ignore_scripts=true
ENV npm_config_min_release_age=2

ENV npm_config_loglevel=verbose

RUN npm install -g --ignore-scripts pnpm@latest-11

RUN mkdir /home/node/app
RUN chown -R node:node /home/node/app

WORKDIR /home/node/app

USER node

RUN pnpm --version
RUN pnpm config list

CMD ["pnpm", "--version"]
