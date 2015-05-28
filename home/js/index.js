// Open external links in new tab
$('a').each(function() {
   if(this.host !== window.location.host) {
   		$(this).attr('target', '_blank');
   }
});

$('body').scrollspy({ target: '#nav-links' });

// set the carousel images to the same height when it's loaded
// not using img onload cause it may not fire in some browsers
(function normalizeCarouselHeight() {
	var height = $('#carousel .item.active img').height();
	if (height === 0) { // img not loaded
		setTimeout(normalizeCarouselHeight, 500);
		return;
	}
	var $images = $('#carousel .item img');

    $images.each(function() {
    	$(this).css("height", height);
    });
})();

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
