var webpack = require('webpack');

module.exports = require('./scalajs.webpack.config');

module.exports.target = 'node';
module.exports.externals = [ { "tessel": "commonjs tessel" } ];

// TODO: compress dependencies but not the output of Scala.js
module.exports.optimization = {
    minimize: false
};
