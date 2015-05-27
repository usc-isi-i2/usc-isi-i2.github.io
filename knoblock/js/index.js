// Open external links in new tab
$('a').each(function() {
   if(this.host !== window.location.host) {
   		$(this).attr('target', '_blank');
   }
});

$('body').scrollspy({ target: '#nav-links' });

// populate teaching section with data from courses.json
d3.json('doc/courses.json', function(error, data) {
	var $courses = $('#teaching .content .courses');

	if (error) {
		$courses.append(error);
		return;
	}

	data.forEach(function(course) {
		var $number = $('<h4/>').addClass('number').text(course.number);
		var $semester = $('<div/>').addClass('semester').text(course.semester);
		var $title = $('<h4/>').addClass('title').text(course.title);
		if (course.syllabus) {
			$link = $('<a/>').attr('href', course.syllabus).attr('target', '_blank').text(course.title);
			$title.empty();
			$title.append($link);
		}

		var $course = $('<div/>').addClass('course');
		$course.append($number);
		$course.append($semester);
		$course.append($title);

		$courses.append($course);
	});
});


$('a[href*=#]:not([href=#])').click(function() {
	// smooth scroll on a large screen
	if ($(window).width() >= 768) {
		if (location.pathname.replace(/^\//,'') === this.pathname.replace(/^\//,'') && location.hostname === this.hostname) {
			var target = $(this.hash);
			if (target.length) {
				$('html, body').animate({
					scrollTop: target.offset().top
				}, 500);
				return false;
			}
		}
	}
});
