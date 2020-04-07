// Open external links in new tab
$('a').each(function() {
   if(this.host !== window.location.host) {
   		$(this).attr('target', '_blank');
   }
});

$('body').scrollspy({ target: '#nav-links' });

// Let header fill the whole window
$('header').css('height', $(window).height());

// add title attribute if name or title overflows
$('#people').find('.name, .title').each(function() {
	if (this.scrollWidth > this.clientWidth) {
		$(this).attr('title', $(this).text());
	}
});

$('a.scroll').click(function() {
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
