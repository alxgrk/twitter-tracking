const path = require('path');

module.exports = {
  entry: {
    main: './src/js/main',
  },
  output: {
    filename: './js/[name].js'
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        include: path.resolve(__dirname, 'src'),
        loader: 'babel-loader'
      }
    ]
  }
};
