FROM node:trixie

RUN npm install --ignore-scripts -g pnpm@latest-10

USER node

RUN mkdir /home/node/app

WORKDIR /home/node/app

ENV npm_config_ignore_scripts=true
ENV npm_config_engine_strict=true

ENV npm_config_minimum_release_age=2880
ENV npm_config_node_linker=hoisted

ENV npm_config_loglevel=info

RUN pnpm config set minimum-release-age 2880

RUN pnpm --version

RUN pnpm init

RUN echo "minimumReleaseAge: 2880" > pnpm-workspace.yaml

RUN pnpm install --save --ignore-scripts @angular/cli

RUN sed -i 's/"test":.*/"ng": "ng"/' package.json

CMD ["pnpm", "run-script", "ng", "version"]
