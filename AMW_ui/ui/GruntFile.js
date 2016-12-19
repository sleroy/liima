/*global module:false*/
module.exports = function(grunt) {
// Project configuration.
grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
// define sass task
    sass: {
            // this is the "dev" Sass config used with "grunt watch" command
            dev: {
                options: {
                    style: 'expanded'
                    // tell Sass to look in the Bootstrap stylesheets directory when compiling
                    //loadPath: 'node_modules/bootstrap-sass/assets/stylesheets'
                },
                files: {
                    // the first path is the output and the second is the input
                    'dist/assets/css/dev.css': 'scss/styles.scss'
                }
            },
            // this is the "production" Sass config used with the "grunt buildcss" command
            dist: {
                options: {
                    style: 'compressed'
                    //loadPath: 'node_modules/bootstrap-sass/assets/stylesheets'
                },
                files: {
                    'dist/assets/css/prod.css': 'scss/styles.scss'
                }
            }
    }
});
// These plugins provide necessary tasks.
// Default task.
grunt.registerTask('default', ['sass']);
grunt.loadNpmTasks('grunt-sass');
};
