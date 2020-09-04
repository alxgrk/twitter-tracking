# twitter-tracking-extension
A browser extension tracking clicks in Twitter - used for my Master's thesis.

*Skeleton taken from [chrome-extension-skeleton](https://github.com/salsita/chrome-extension-skeleton)*

## Build bundles for Chrome and Firefox

To bundle the applications, run [bundle-for-firefox-and-chrome.sh](./bundle-for-firefox-and-chrome.sh).
The resulting artifacts will be uploaded as part of a release.

## Updates

Firefox offers the ability to self-host addons. This is the case here, so [firefox-addon-update.json](./firefox-addon-update.json) always needs to be available under the same URL.
