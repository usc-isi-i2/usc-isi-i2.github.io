module.exports = function(grunt) {

	grunt.initConfig({

		clean: {
			html: ['*.html'],
			css: ['src/css/*.css', 'css/*.css'],
			js: ['js/*.js']
		},

		htmllint: {
			all: ['src/*.html']
		},

		htmlmin: {
			options: {
				removeComments: true,
				collapseWhitespace: true
			},
			multiple: {
				expand: true,
				flatten: true,
				src: 'src/*.html',
				dest: ''
			}
		},

		sass: {
			options: {
				style: 'expanded',
				sourcemap: 'none'
			},
			multiple: {
				expand: true,
				flatten: true,
				src: 'src/sass/*.scss',
				dest: 'src/css',
				ext: '.css'
			}
		},

		autoprefixer: {
			multiple: {
				expand: true,
				flatten: true,
				src: 'src/css/*.css',
				dest: 'src/css',
			}
		},

		cssmin: {
			multiple: {
				expand: true,
				flatten: true,
				src: 'src/css/*.css',
				dest: 'css',
			}
		},

		jshint: {
			all: 'src/js/*.js',
			options: {
				curly: true,
				eqnull: true,
				eqeqeq: true,
				browser: true
			}
		},

		uglify: {
			js: {
				expand: true,
				cwd: 'src/js',
				src: '*.js',
				dest: 'js'
			}
		},

		watch: {
			options: {
				livereload: true,
			},
			html: {
				files: ['src/*.html'],
				tasks: ['clean:html', 'htmllint', 'htmlmin']
			},
			sass: {
				files: ['src/sass/*.scss'],
				tasks: ['clean:css', 'sass', 'autoprefixer', 'cssmin']
			},
			js: {
				files: ['src/js/*.js'],
				tasks: ['clean:js', 'jshint', 'uglify']
			}
		}
		
	});

grunt.loadNpmTasks('grunt-contrib-clean');
grunt.loadNpmTasks('grunt-html');
grunt.loadNpmTasks('grunt-contrib-htmlmin');
grunt.loadNpmTasks('grunt-contrib-sass');
grunt.loadNpmTasks('grunt-autoprefixer');
grunt.loadNpmTasks('grunt-contrib-cssmin');
grunt.loadNpmTasks('grunt-contrib-jshint');
grunt.loadNpmTasks('grunt-contrib-uglify');
grunt.loadNpmTasks('grunt-contrib-watch');

grunt.registerTask('default', ['watch']);
grunt.registerTask('all', ['clean', 'htmllint', 'htmlmin', 'sass', 'autoprefixer', 'cssmin', 'jshint', 'uglify']);

};