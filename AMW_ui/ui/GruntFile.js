/*global module:false*/
module.exports = function (grunt) {
    // Project configuration.
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        // define clean task
        clean: {
            pre: ['dist/'],
            post: ['dist/assets/css/prod.css']
        },
        // define copy task
        copy: {
            main: {
                expand: true,
                cwd: 'src',
                src: '*',
                dest: 'dist/',
                filter: 'isFile'
            },
        },
        // define sass task
        sass: {
            dev: {
                options: {
                    outputStyle: 'expanded'
                },
                files: {
                    // the first path is the output and the second is the input
                    'dist/assets/css/dev.css': 'src/scss/styles.scss'
                }
            },
            prod: {
                options: {
                    outputStyle: 'compressed'
                },
                files: {
                    // the first path is the output and the second is the input
                    'dist/assets/css/prod.css': 'src/scss/styles.scss'
                }
            }
        },
        // define autoprefixer task
        autoprefixer: {
            options: {
                browsers: ['last 8 versions']
            },
            dev: {
                files: {
                    'dist/assets/css/dev.css': 'dist/assets/css/dev.css'
                }
            },
            prod: {
                files: {
                    'dist/assets/css/prod.css': 'dist/assets/css/prod.css'
                }
            }

        },
        // define chache buster task
        asset_cachebuster: {
            options: {
                buster: Date.now()
            },
            build: {
                files: {
                    'dist/assets/css/main.css': ['dist/assets/css/prod.css'],
                    'dist/index.html': ['src/index.html']
                }
            }
        }
    });
    // These plugins provide necessary tasks.
    // Default task.
    grunt.registerTask('default', ['clean:pre', 'copy', 'sass:prod', 'autoprefixer:prod', 'asset_cachebuster', 'clean:post']);
    grunt.registerTask('dev', ['clean:pre', 'copy', 'sass:dev', 'autoprefixer:dev']);
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-autoprefixer');
    grunt.loadNpmTasks('grunt-sass');
    grunt.loadNpmTasks('grunt-asset-cachebuster');
};
