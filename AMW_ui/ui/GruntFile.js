/*global module:false*/
module.exports = function (grunt) {
    // Project configuration.
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        // define clean task
        clean: {
            pre: ['dist/'],
            post: ['dist/assets/css/prod.css', 'dist/assets/css/dev.css']
        },
        // define copy task
        copy: {
            main: {
                expand: true,
                cwd: 'src',
                src: '*',
                dest: 'dist/',
                filter: 'isFile'},
            fonts: {
                expand: true,
                cwd: 'node_modules/bootstrap-sass/assets/fonts/bootstrap',
                src: '*',
                dest: 'dist/assets/fonts',
                filter: 'isFile'},
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
        uglify: {
            options: {
                manage: false,
                preserveComments: 'all' //preserve all comments on JS files
            },
            my_target: {
                files: {
                    'dist/assets/js/main.min.js': ['node_modules/bootstrap-sass/assets/javascripts/bootstrap.js', 'src/js/*.js', 'node_modules/bootstrap-tagsinput/dist/bootstrap-tagsinput.js']
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
        // define watch task
        watch: {
            css: {
                files: 'src/scss/*.scss',
                tasks: ['sass:dev'],
                options: {
                    livereload: true,
                },
            },
        },
        // define chache buster task
        asset_cachebuster: {
            options: {
                buster: Date.now()
            },
            build: {
                files: {
                    'dist/assets/css/main.css': ['dist/assets/css/prod.css'],
                    'dist/index.html': ['src/index.html'],
                    'dist/edit-resource.html': ['src/edit-resource.html'],
                    'dist/resources.html': ['src/resources.html']
                }
            }
        }
    });
    // These plugins provide necessary tasks.
    // Default task.
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-autoprefixer');
    grunt.loadNpmTasks('grunt-sass');
    grunt.loadNpmTasks('grunt-asset-cachebuster');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.registerTask('default', ['clean:pre', 'copy:main', 'copy:fonts', 'sass:prod', 'autoprefixer:prod', 'uglify', 'asset_cachebuster', 'clean:post']);
    grunt.registerTask('dev', ['clean:pre', 'copy', 'sass:dev', 'autoprefixer:dev']);
};
