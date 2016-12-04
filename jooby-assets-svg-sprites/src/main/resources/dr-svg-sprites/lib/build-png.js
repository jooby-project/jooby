var fs = require("fs");

var async = require("async");
var svg2png = require("svg2png");

module.exports = function (sprite, callback) {
	// Items not found
	if (sprite.items.length === 0) {
		return ("function" === typeof callback) ? callback() : undefined;
	}

	var tasks = sprite.sizes.map(function (size) {
		return function (callback) {
			const input = fs.readFileSync(sprite.svgPath);
			svg2png(input, {width: size.width})
			.then(
				output => fs.writeFileSync(size.pngPath, output)
			)
			.then(
				() => callback(null, size.pngPath)
			)
			.catch(callback);
		};
	});

	async.parallel(tasks, callback);

};
