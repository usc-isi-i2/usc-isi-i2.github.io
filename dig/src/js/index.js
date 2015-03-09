// Open external links in new tab
$('a').each(function() {
   if(this.host !== window.location.host) {
   		$(this).attr("target", "_blank");
   }
});

function adjustSlidesHeight() {
	var width = $(".slides").width();
	var height = width * 0.8;

	if (height > $(window).height()) {
		height = $(window).height();
	}

	$(".slides").css("height", height + "px");
}

adjustSlidesHeight();

$(window).on("resize", adjustSlidesHeight);

