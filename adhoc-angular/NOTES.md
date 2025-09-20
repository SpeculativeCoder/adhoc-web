# WIP


## npm ci

MSYS_NO_PATHCONV=1 \
docker run \
-v ./.npm:/home/node/.npm \
-v ./node_modules:/home/node/app/node_modules \
-v ./package.json:/home/node/app/package.json:ro \
-v ./package-lock.json:/home/node/app/package-lock.json:ro \
-w /home/node/app \
-e npm_config_loglevel=info \
-i -t --rm \
-u node \
node:trixie \
npm ci --ignore-scripts


## npm audit

MSYS_NO_PATHCONV=1 \
docker run \
-v ./.npm:/home/node/.npm \
-v ./node_modules:/home/node/app/node_modules:ro \
-v ./package.json:/home/node/app/package.json:ro \
-v ./package-lock.json:/home/node/app/package-lock.json:ro \
-w /home/node/app \
-e npm_config_loglevel=info \
-i -t --rm \
-u node \
node:trixie \
npm audit


## npm run build

MSYS_NO_PATHCONV=1 \
docker run \
-v ./.angular:/home/node/app/.angular:ro \
-v ./.npm:/home/node/.npm \
-v ./dist:/home/node/app/dist \
-v ./node_modules:/home/node/app/node_modules:ro \
-v ./src:/home/node/app/src:ro \
-v ./angular.json:/home/node/app/angular.json:ro \
-v ./package.json:/home/node/app/package.json:ro \
-v ./package-lock.json:/home/node/app/package-lock.json:ro \
-v ./tsconfig.app.json:/home/node/app/tsconfig.app.json:ro \
-v ./tsconfig.json:/home/node/app/tsconfig.json:ro \
-v ./tsconfig.spec.json:/home/node/app/tsconfig.spec.json:ro \
-w /home/node/app \
-e npm_config_loglevel=info \
-i -t --rm \
-u node \
node:trixie \
npm run build -- --configuration=jsdom_fix,development,customization,en-US


## npm version

MSYS_NO_PATHCONV=1 \
docker run \
-v ./.npm:/home/node/.npm \
-w /home/node/app \
-e npm_config_loglevel=info \
-i -t --rm \
-u node \
node:trixie \
npm --version

