const _ = require('lodash');
const path = require('path');
const webpack = require('webpack');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

const config = require('./config.js');


module.exports = _.merge({}, config, {
    mode: "development",
    output: {
        path: path.resolve(__dirname, '../build/dev'),
    },
    devtool: 'source-map',
    plugins: [
        new CleanWebpackPlugin(),
        new CopyWebpackPlugin(
            {
                patterns: [{
                    from: './src',
                    globOptions: {
                        ignore: ['**/*.js'],
                    },
                    transform(content, absoluteFrom) {
                        console.log(absoluteFrom)
                        if (absoluteFrom.includes("manifest.json")) {
                            return content.toString().replace(
                                "$ACCESS_SITE_PERMISSION",
                                "*://localhost/*");
                        } else {
                            return content
                        }
                    }
                }]
            }),
        new webpack.DefinePlugin({
            API: JSON.stringify("http://localhost:8080/events")
        })
    ],
    watch: true
});
