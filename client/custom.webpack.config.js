var webpack = require('webpack');
var path = require('path');

module.exports = require('./scalajs.webpack.config');

module.exports.resolve ||= {};
module.exports.resolve.alias ||= {};
module.exports.resolve.alias['chart.js'] = path.resolve(
    __dirname,
    'node_modules',
    'chart.js',
    'auto',
    'auto.js'
);
module.exports.resolve.alias['moment.js'] = path.resolve(
    __dirname,
    'node_modules',
    'moment',
    'min',
    'moment.min.js'
);

module.exports.plugins ||= [];
module.exports.plugins.push(
    new webpack.IgnorePlugin({
        resourceRegExp: /^\.\/locale$/,
        contextRegExp: /moment$/,
    })
)
