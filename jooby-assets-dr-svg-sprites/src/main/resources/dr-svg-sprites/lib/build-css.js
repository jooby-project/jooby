var fs = require("fs");
var path = require("path");
var handlebars = require("handlebars");
var util = require("./util");

handlebars.registerHelper("url", function (filepath, relation) {
	if (path.basename(relation).indexOf(".") > -1) {
		relation = path.dirname(relation);
	}
	return path.relative(relation, filepath).replace(/\\/g, "/");
});

handlebars.registerHelper("unit", function (value, unit, baseFontSize, modifier) {
	if (typeof modifier === "number") {
		value *= modifier;
	}
	if (unit == "rem" || unit == "em") {
		value = value / baseFontSize;
	}
	return value + ((value === 0) ? "" : unit);
});

handlebars.registerHelper("prefix", function (items, prefix) {
	return prefix + items.map(function (item) {
		return item.selector || item.className;
	}).join(", " + prefix);
});

handlebars.registerHelper("prefixAll", function (sizes, prefix) {
	return sizes.map(function (size) {
		return handlebars.helpers.prefix.apply(this, [size.items, prefix]);
	}.bind(this)).join(", ");
});

module.exports = function (sprite, callback) {

	fs.readFile(sprite.config.template, "utf-8", function (err, template) {
		if (err) {
			throw err;
		}

		var source;

		if (sprite.items.length === 0) {
			source = '/* Not found any SVG file... */'
		} else {
			var compiler = handlebars.compile(template);
			source = compiler(sprite);
		}

		util.write(sprite.cssPath, source, callback);
	});

};
