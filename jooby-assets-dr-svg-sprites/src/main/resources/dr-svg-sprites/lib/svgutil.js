var fs = require("fs");
var path = require("path");
var SVGO = require("svgo");


function parse (content, config, callback) {
	var svgo = new SVGO(config);
	svgo.optimize(content, function (result) {
		var namespaces = {};
		var matches = result.data.match(/xmlns:[^=]+="[^"]+"/g);
		if (matches) {
			matches.forEach(function (match) {
				var split = match.split("=");
				namespaces[split[0]] = split[1].slice(1, -1);
			});
		}
		var width = parseFloat(result.info.width);
		var height = parseFloat(result.info.height);
		if (!width || !height) {
			var viewbox = content.match(/viewbox="([^"]+)"/i);
			if (viewbox) {
				viewbox = viewbox[1].split(/[^\.\d-]+/);
				if (!width) {
					width = parseFloat(viewbox[2]);
				}
				if (!height) {
					height = parseFloat(viewbox[3]);
				}
			}
		}
		var data = {
			source: result.data.replace(/^<svg[^>]+>|<\/svg>$/g, ""),
			namespaces: namespaces,
			width: width,
			height: height
		}
		callback(null, data);
	});
}

function transform (data, x, y, fill) {
	if (x == 0 && y == 0) {
		return data;
	}
	if (data != data.match(/^<g>(?:.*?)<\/g>/)) {
		data = "<g>" + data + "</g>";
	}
	var attributes = " transform=\"translate(" + x + ( y ? " " + y : "" ) + ")\"";
	if (fill) {
		if (data.match(/fill="/)) {
			data = data.replace(/(fill=")[^"]+(")/g, "$1" + fill + "$2");
		}
		else {
			attributes += " fill=\"" + fill + "\"";
		}
	}
	data = data.replace(/^<g/, "<g" + attributes);
	return data;
}

function wrap (width, height, shapes, attributes) {
	var attrs = [];
	var name, value;
	for (name in attributes) {
		var value = attributes[name];
		if (value) {
			attrs.push(" " + name + "=\"" + value + "\"");
		}
	}
	return '<svg' + attrs.join("") + ' viewBox="0 0 ' + width + ' ' + height + '" width="' + width + '" height="' + height + '">' + shapes.join("") + '</svg>';
}

module.exports.parse = parse;

module.exports.transform = transform;

module.exports.wrap = wrap;