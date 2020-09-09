const _ = require('lodash');
const path = require('path');
const webpack = require('webpack');
const dotenv = require('dotenv').config({path: path.resolve(__dirname, '..', '.env')});
const CopyWebpackPlugin = require('copy-webpack-plugin');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');

const config = require('./config.js');

let distRoot = path.resolve(__dirname, '..', 'build', 'prod');
module.exports = _.merge({}, config, {
    mode: "production",
    output: {
        path: distRoot,
    },
    optimization: {
        minimize: true,
        minimizer: [new TerserPlugin()],
    },
    // devtool: 'eval',
    plugins: [
        new CleanWebpackPlugin(),
        new CopyWebpackPlugin({
            patterns: [
                {
                    from: 'src',
                    globOptions: {
                        ignore: ['**/*.js'],
                    },
                    transform(content, absoluteFrom) {
                        if (absoluteFrom.includes("manifest.json")) {
                            return content.toString()
                                .replace("$VERSION", process.env.npm_package_version)
                                .replace("$ACCESS_SITE_PERMISSION", dotenv.parsed.PROD_API_URL);
                        } else {
                            return content
                        }
                    }
                }, {
                    from: 'node_modules/webextension-polyfill/dist/browser-polyfill.js',
                    to: path.resolve(distRoot, 'js')
                }
            ]
        }),
        new webpack.DefinePlugin({
            'process.env.NODE_ENV': '"production"',
            API: JSON.stringify(dotenv.parsed.PROD_API_URL)
        })
    ]
});
