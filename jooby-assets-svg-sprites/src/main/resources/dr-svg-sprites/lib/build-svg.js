var _ = require("lodash");
var SVGO = require("svgo");
var util = require("./util");
var svgutil = require("./svgutil");

module.exports = function (sprite, callback) {
	// Items not found
	if (sprite.items.length === 0) {
		return ("function" === typeof callback) ? callback() : undefined;
	}
	
	var items = sprite.items.map(function (item) {
		return svgutil.transform(item.source, item.x, item.y);
	});

	var attributes = {};
	_.assign(attributes, sprite.config.svgAttributes);
	_.assign(attributes, sprite.namespaces);

	var source = svgutil.wrap(sprite.width, sprite.height, items, attributes);

	var svgo = new SVGO(sprite.config.svgo);
	svgo.optimize(source, function (result) {
		util.write(sprite.svgPath, result.data, callback);
	});

};
