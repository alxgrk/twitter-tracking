const path = require('path');

module.exports = {
  entry: {
    main: './src/js/main',
    "tracked-events": './src/popup/tracked-events'
  },
  output: {
    filename: (pathData) => pathData.chunk.name !== 'main' ? './popup/[name].js' : './js/[name].js'
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
