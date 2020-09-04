#!/bin/bash

WEB_EXT="./node_modules/.bin/web-ext -a build/prod/ -s build/prod/"
[[ -f ./node_modules/.bin/web-ext ]] || echo "Make sure you have 'web-ext' build tool installed." || exit 1

source .env
npm run build

# bundle firefox addon
$WEB_EXT sign --api-key $JWT_ISSUER --api-secret $JWT_SECRET
$WEB_EXT build --overwrite-dest

# bundle chrome extension
chromium --pack-extension=build/prod/ --pack-extension-key=mykey.pem
