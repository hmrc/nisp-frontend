module.exports = {
  plugins: [
    require('autoprefixer')(),
    require('postcss-merge-selectors')({
      selectorFilter: /.govuk-*/,
      promote: true,
    }),
    require('postcss-em-media-query')(),
    require('postcss-combine-media-query')(),
    require('postcss-mq-optimize')(),
    require('postcss-filter-mq')({
      invert: false,
      keepBaseRules: true,
    }),
    require('css-mqpacker-sort-mediaqueries')(),
    require('css-declaration-sorter')({
      order: 'alphabetical',
    }),
    require('postcss-combine-duplicated-selectors')({
      removeDuplicatedValues: true,
    }),
    require('postcss-reporter')({
      positionless: 'last',
    }),
    require('postcss-zindex')(),
    require('cssnano')({
      preset: 'default',
    }),
  ],
};
