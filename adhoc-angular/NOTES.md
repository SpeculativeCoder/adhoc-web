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


## pnpm config

MSYS_NO_PATHCONV=1 \
docker build -t node_pnpm ./node_pnpm/. \
&& \
MSYS_NO_PATHCONV=1 \
docker run \
-i -t --rm \
node_pnpm \
bash -c "pnpm --version && pnpm config list"


## pnpm import

MSYS_NO_PATHCONV=1 \
docker build -t node_pnpm ./node_pnpm/. \
&& \
MSYS_NO_PATHCONV=1 \
docker run \
-v ./package.json:/home/node/app/package.json:ro \
-v ./package-lock.json:/home/node/app/package-lock.json:ro \
-v ./pnpm-lock.yaml:/home/node/app/pnpm-lock.yaml.bind \
-i -t --rm \
node_pnpm \
bash -c "pnpm import && cat pnpm-lock.yaml > pnpm-lock.yaml.bind"


## pnpm audit

MSYS_NO_PATHCONV=1 \
docker build -t node_pnpm ./node_pnpm/. \
&& \
MSYS_NO_PATHCONV=1 \
docker run \
-v ./package.json:/home/node/app/package.json:ro \
-v ./pnpm-lock.yaml:/home/node/app/pnpm-lock.yaml:ro \
-i -t --rm \
node_pnpm \
pnpm audit


## pnpm ci

MSYS_NO_PATHCONV=1 \
docker build -t node_pnpm ./node_pnpm/. \
&& \
MSYS_NO_PATHCONV=1 \
docker run \
-v ./node_modules:/home/node/app/node_modules \
-v ./package.json:/home/node/app/package.json:ro \
-v ./pnpm-lock.yaml:/home/node/app/pnpm-lock.yaml:ro \
-i -t --rm \
node_pnpm \
pnpm install --frozen-lockfile --ignore-scripts


## pnpm run build

MSYS_NO_PATHCONV=1 \
docker build -t node_pnpm ./node_pnpm/. \
&& \
MSYS_NO_PATHCONV=1 \
docker run \
-v ./.angular:/home/node/app/.angular:ro \
-v ./dist:/home/node/app/dist \
-v ./node_modules:/home/node/app/node_modules:ro \
-v ./src:/home/node/app/src:ro \
-v ./angular.json:/home/node/app/angular.json:ro \
-v ./package.json:/home/node/app/package.json:ro \
-v ./tsconfig.app.json:/home/node/app/tsconfig.app.json:ro \
-v ./tsconfig.json:/home/node/app/tsconfig.json:ro \
-v ./tsconfig.spec.json:/home/node/app/tsconfig.spec.json:ro \
-i -t --rm \
node_pnpm \
pnpm run build --configuration=jsdom_fix,development,customization,en-US


## pnpm run serve

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
-v ./tsconfig.app.json:/home/node/app/tsconfig.app.json:ro \
-v ./tsconfig.json:/home/node/app/tsconfig.json:ro \
-v ./tsconfig.spec.json:/home/node/app/tsconfig.spec.json:ro \
-p 127.0.0.1:4200:4200/tcp \
-i -t --rm \
node_pnpm \
pnpm run start --host 0.0.0.0

#--configuration=jsdom_fix,development,customization,en-US


## TODO REUSE CACHE

/home/node/.local/share/pnpm/store/v10
/home/node/.cache/pnpm/metadata-v1.3
