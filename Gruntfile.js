module.exports = function(grunt){
  grunt.initConfig({
    // Builds libsass
    sass: {
      dev: {
        options: {
          style: "expanded"
        },
        files: [{
          expand: true,
          cwd: "app/uk/gov/hmrc/nisp/assets/sass",
          src: ["*.scss"],
          dest: "public/stylesheets/",
          ext: ".css"
        }]
      }
    },
    scsslint: {
      allFiles: [
        'app/uk/gov/hmrc/nisp/assets/sass'
      ],
      options: {
        bundleExec: false,
        config: '.scss-lint.yaml',
        compact:true,
        colorizeOutput:true,
        maxBuffer:3000*1024,
        reporterOutput: 'logs/reports/scss-lint-report.xml'
      }
    },
    // Watches styles and specs for changes
    watch: {
      css: {
        files: ['app/uk/gov/hmrc/nisp/assets/sass/**/*.scss'],
        tasks: ['sass'],
        options: { nospawn: true }
      }
    },
    clean: ["public/stylesheets"],
    plato: {
        nisp_js_report: {
          files: {
            'logs/reports': ['public/**/*.js']
          }
        }
  }
  });

  [
    'grunt-contrib-watch',
    'grunt-contrib-sass',
    'grunt-contrib-clean',
    'grunt-plato',
    'grunt-scss-lint'
  ].forEach(function (task) {
    grunt.loadNpmTasks(task);
  });

  grunt.registerTask('build', ['clean','sass','plato'])
  grunt.registerTask('default', ['clean','sass','plato']);
};
