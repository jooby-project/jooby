"use strict";

var GrowingPacker = require('./packer.growing.js');

module.exports = function(items, options) {
	options = options || {};
	var packer = new GrowingPacker();
	var inPlace = options.inPlace || false;

	// Clone the items.
	var newItems = items.map(function(item) { return inPlace ? item : { width: item.width, height: item.height, item: item }; });

	newItems = newItems.sort(function(a, b) {
		// TODO: check that each actually HAS a width and a height.
		// Sort based on the size (area) of each block.
		return (b.width * b.height) - (a.width * a.height);
	});

	packer.fit(newItems);

	var w = newItems.reduce(function(curr, item) { return Math.max(curr, item.x + item.width); }, 0);
	var h = newItems.reduce(function(curr, item) { return Math.max(curr, item.y + item.height); }, 0);

	var ret = {
		width: w,
		height: h
	};

	if (!inPlace) {
		ret.items = newItems;
	}

	return ret;
};
