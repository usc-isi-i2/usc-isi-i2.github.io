module.exports = function(grunt) {

	grunt.initConfig({

		clean: {
			css: ['css/*.css'],
		},

		sass: {
			options: {
				style: 'expanded',
				sourcemap: 'none'
			},
			multiple: {
				expand: true,
				flatten: true,
				src: 'sass/*.scss',
				dest: 'css',
				ext: '.css'
			}
		},

		autoprefixer: {
			multiple: {
				expand: true,
				flatten: true,
				src: 'css/*.css',
				dest: 'css',
			}
		},

		jshint: {
			all: 'js/*.js',
			options: {
				curly: true,
				eqnull: true,
				eqeqeq: true,
				browser: true
			}
		},

		watch: {
			options: {
				livereload: true,
			},
			html: {
				files: ['*.html'],
			},
			sass: {
				files: ['sass/*.scss'],
				tasks: ['clean:css', 'sass', 'autoprefixer'],
			},
			js: {
				files: ['js/*.js'],
				tasks: ['jshint'],
			}
		}
		
	});

	grunt.loadNpmTasks('grunt-contrib-clean');
	grunt.loadNpmTasks('grunt-contrib-sass');
	grunt.loadNpmTasks('grunt-autoprefixer');
	grunt.loadNpmTasks('grunt-contrib-jshint');
	grunt.loadNpmTasks('grunt-contrib-watch');

	grunt.registerTask('default', ['watch']);
	grunt.registerTask('all', ['clean', 'sass', 'autoprefixer', 'jshint']);

};