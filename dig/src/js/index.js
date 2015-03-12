// Open external links in new tab
$('a').each(function() {
   if(this.host !== window.location.host) {
   		$(this).attr('target', '_blank');
   }
});

var navBarHeight = $('.dig-nav-bar').outerHeight();

// smooth scroll
$('a[href*=#]:not([href=#])').click(function() {
	if (location.pathname.replace(/^\//,'') === this.pathname.replace(/^\//,'') && location.hostname === this.hostname) {
		var target = $(this.hash);
		if (target.length) {
			$('html, body').animate({
				scrollTop: target.offset().top - navBarHeight
			}, 500);
			return false;
		}
	}
});
$('a[href=#]').click(function() {
	$('html, body').animate({
		scrollTop: 0
	}, 500);
	return false;
});