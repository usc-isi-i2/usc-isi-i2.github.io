// Open external links in new tab
$('a').each(function() {
   if(this.host !== window.location.host) {
   		$(this).attr('target', '_blank');
   }
});

// Let header fill the whole window
$('header').css('height', $(window).height());

// Adjust slides height
$('.slides').css('height', $('.slides').width() * 0.8);

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

// scroll spy
$(window).on("scroll resize", function() {
	var mid = $(window).scrollTop() + $(window).height() / 2;

    $(".area").each(function() {
    	var elemTop = $(this).offset().top;
    	var elemBottom = elemTop + $(this).outerHeight();

    	if (elemTop < mid && elemBottom > mid) {
    		$(".dig-nav-bar .active").removeClass("active");
    		var hash = "#" + $(this).attr("id");
    		$(".dig-nav-bar a[href=" + hash + "]").addClass("active");
    	}
    });
});

// add title attribute if name or title overflows
$('#people').find('.name, .title').each(function() {
	if (this.scrollWidth > this.clientWidth) {
		$(this).attr('title', $(this).text());
	}
});