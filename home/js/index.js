// Open external links in new tab
$('a').each(function() {
   if(this.host !== window.location.host) {
   		$(this).attr('target', '_blank');
   }
});

$('body').scrollspy({ target: '#nav-links' });

// set the carousel images to the same height when it's loaded
$('#carousel .item.active img').load(function normalizeCarouselHeight() {
	var height = $(this).height();
	var $images = $('#carousel .item img');

    $images.each(function() {
    	$(this).css("height", height);
    });
});

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
