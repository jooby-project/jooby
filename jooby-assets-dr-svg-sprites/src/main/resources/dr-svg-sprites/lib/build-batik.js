/**
 * Modified version build-png that replace panthomjs with batik and simplify multi-platform dist.
 * svg2png function is provided it by j2v8. see build-png to compare with the original file.
 */
var async = require("async");

module.exports = function (sprite, callback) {
	// Items not found
	if (sprite.items.length === 0) {
		return ("function" === typeof callback) ? callback() : undefined;
	}

	var tasks = sprite.sizes.map(function (size) {
		return function (callback) {
			svg2png(sprite.svgPath, size.pngPath, size.width, size.height, function (err) {
				if (err) {
					throw err;
				}
				callback(null, size.pngPath);
			});
		};
	});
	
	async.parallel(tasks, callback);
	
};
