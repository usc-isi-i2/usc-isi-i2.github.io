var bibtex = $("#word-cloud-container").data("bib");

d3.text(bibtex, function(error, data) {
	if (error) {
		console.warn(error);
		return;
	}
	var text = "";
	var entries = BibtexParser(data).entries;
	entries.forEach(function(entry) {
		text += entry.Fields.title || entry.Fields.Title;
		text += " ";
	});

	var wordCount = {};
	text.trim().split(/\W+/).forEach(function(word){
		// capitalize the word
		word = word[0].toUpperCase() + word.slice(1);
		if (wordCount[word]) {
			wordCount[word] += 1;
		}
		else {
			wordCount[word] = 1;
		}
	});

	var wordCountList = [];
	for (var word in wordCount) {
		if (commonWords.indexOf(word.toLowerCase()) !== -1) {
			continue; // skip common words
		}
		wordCountList.push({
			"word": word,
			"count": wordCount[word]
		});
	}

	wordCountList.sort(function(a, b) {
		if (a.count > b.count) {
			return 1;
		}
		else if (a.count < b.count) {
			return -1;
		}
		return 0;
	});

	// extract the 50 most frequent words
	wordCountList = wordCountList.slice(-50);

	createWordCloud(wordCountList);
});

function createWordCloud(data) {
	// Construct the word cloud's SVG element
	var svg = d3.select("#word-cloud-container").append("svg");
	var width = 600;
	var height = 250;

	svg
	.attr("viewBox", "0 0 " + width + " " + height)
	.append("g")
	.attr("transform", "translate(" + width/2 + "," + height/2 +")")

	// map word count to word font size
	var config = $("#word-cloud-container").data();
	var minWordCount = config.minWordCount;
	var maxWordCount = config.maxWordCount;
	var minFontSize = config.minFontSize;
	var maxFontSize = config.maxFontSize;
	var count2size = d3.scale.sqrt().domain([minWordCount, maxWordCount]).range([minFontSize, maxFontSize]);

	d3.layout.cloud()
	.size([width, height])
	.words(data)
	// .timeInterval(10)
	.rotate(function(d) {
		radian = Math.random();
		return ~~(radian * 1.35) * 90;
	})
	// .padding(2)
	.text(function(d) { return d.word; })
	.font('"Helvetica Neue", Helvetica, Arial, sans-serif')
	.fontSize(function(d) { return count2size(d.count); })
	.fontWeight(function() { return "bold"; })
	.on("end", draw)
	.start();

	// Draw the word cloud
	function draw(words) {
		var cloud = svg.select("g").selectAll("text").data(words);
		var colors = d3.scale.category20c().range();
		d3.shuffle(colors);

		// Entering word
		cloud.enter().append("text")
		.style("fill", function(d, i) { return d3.rgb(colors[i % colors.length]).darker(1); })
		.style("font-family", '"Helvetica Neue", Helvetica, Arial, sans-serif')
		.style("font-size", function(d) { return d.size + "px"; })
		.style("font-weight", "bold")
		.attr("text-anchor", "middle")
		.text(function(d) { return d.text; })
		.attr("transform", function(d) {
			return "translate(" + [d.x, d.y] + ")rotate(" + d.rotate + ")";
		});
	}
}

var commonWords = [
	'a',
	'about',
	'an',
	'and',
	'are',
	'as',
	'by',
	'for',
	'from',
	'in',
	'into',
	'my',
	'not',
	'of',
	'on',
	'or',
	'that',
	'the',
	'to',
	'through',
	'when',
	'where',
	'which',
	'why',
	'with',
	'your',
];