var assert = require('assert');

var pack = require('../index.js');

function intersect(item1, item2) {
	var item1vertices = [
		{ x: item1.x, y: item1.y }, // Top left
		{ x: item1.x + item1.width, y: item1.y }, // Top right
		{ x: item1.x + item1.width, y: item1.y + item1.height }, // Bottom right
		{ x: item1.x, y: item1.y + item1.h } // Bottom left
	];

	for (var i = 0; i < item1vertices.length; i++) {
		var vertex = item1vertices[i];

		if (item2.x < vertex.x && vertex.x < item2.x + item2.width) {
			if (item2.y < vertex.y && vertex.y < item2.y + item2.height) {
				return true;
			}
		}
	}

	return false;
}

function verifyResult(result, items) {
	assert.ok(result != null, "Result is defined");
	assert.ok(items != null, "Items is defined");
	assert.ok('width' in result, "Result has a width");
	assert.ok('height' in result, "Result has a height");

	for (var i = 0; i < items.length; i++) {
		var item = items[i];

		assert.ok('x' in item, "Item " + i + " has an x coordinate");
		assert.ok('y' in item, "Item " + i + " has a y coordinate");
		assert.ok('width' in item, "Item " + i + " has a width");
		assert.ok('height' in item, "Item " + i + " has a height");

		assert.ok(item.x >= 0, "Item is within the box (left)");
		assert.ok(item.y >= 0, "Item is within the box (top)");
		assert.ok(item.x + item.width <= result.width, "Item is within the box (right)");
		assert.ok(item.y + item.height <= result.height, "Item is within the box (bottom)");

		for (var j = 0; j < items.length; j++) {
			var otheritem = items[j];
			assert.ok(!intersect(item, otheritem), "Item " + i + " does not intersect with item " + j);
		}
	}
}

describe('bin-pack with no options', function() {
	it('packs properly (basic)', function() {
		var bins = [
			{ width: 10, height: 10 },
			{ width: 10, height: 10 },
			{ width: 10, height: 10 },
			{ width: 10, height: 10 }
		];

		var result = pack(bins);
		assert.ok('items' in result, "Result has items");
		assert.equal(result.items.length, bins.length, "Result has same amount of items as the source");
		verifyResult(result, result.items);
	});

	it('packs properly when items are passed in in the wrong order', function() {
		var bins = [
			{ width: 10,   height: 10 },
			{ width: 100,  height: 100 },
			{ width: 1000, height: 1000 }
		];

		var result = pack(bins);
		assert.ok('items' in result, "Result has items");
		assert.equal(result.items.length, bins.length, "Result has same amount of items as the source");
		verifyResult(result, result.items);
	});

	it('packs properly when items are irregular', function() {
		var bins = [
			{ width: 10,  height: 110 },
			{ width: 100, height: 10 },
			{ width: 20,  height: 1 },
			{ width: 4,   height: 48 }
		];

		var result = pack(bins);
		assert.ok('items' in result, "Result has items");
		assert.equal(result.items.length, bins.length, "Result has same amount of items as the source");
		verifyResult(result, result.items);
	});

	it('does not affect original items', function() {
		var bins = [
			{ width: 10, height: 10 },
			{ width: 10, height: 10 },
			{ width: 10, height: 10 },
			{ width: 10, height: 10 }
		];

		var result = pack(bins);
		assert.ok(!('x' in bins[0]), "Original bin does not have x property");
		assert.ok(!('y' in bins[0]), "Original bin does not have y property");
	});
});

describe('bin-pack with in place option', function() {
	it('packs properly (basic)', function() {
		var bins = [
			{ width: 10, height: 10 },
			{ width: 10, height: 10 },
			{ width: 10, height: 10 },
			{ width: 10, height: 10 }
		];

		var result = pack(bins, { inPlace: true });
		assert.ok(!('items' in result), "Result does not have items");
		verifyResult(result, bins);
	});

	it('affects original items', function() {
		var bins = [
			{ width: 10, height: 10 },
			{ width: 10, height: 10 },
			{ width: 10, height: 10 },
			{ width: 10, height: 10 }
		];

		var result = pack(bins, { inPlace: true });
		assert.ok('x' in bins[0], "Original bin has an x property");
		assert.ok('y' in bins[0], "Original bin has a y property");
	});

	it('does not sort original items', function() {
		var bin0 = { width: 10,   height: 10 };
		var bin1 = { width: 100,  height: 100 };
		var bin2 = { width: 1000, height: 1000 };

		var bins = [
			bin0,
			bin1,
			bin2
		];

		var result = pack(bins, { inPlace: true });
		assert.ok(bins[0] === bin0, "Bin 0 is still in place 0");
		assert.ok(bins[1] === bin1, "Bin 1 is still in place 1");
		assert.ok(bins[2] === bin2, "Bin 2 is still in place 2");
		verifyResult(result, bins);
	});
});
