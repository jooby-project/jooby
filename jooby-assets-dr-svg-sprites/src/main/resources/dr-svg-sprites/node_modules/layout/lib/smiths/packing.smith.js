function PackingSmith(algorithm, options) {
  // Define items and save algorithm for later
  this.items = [];
  this.algorithm = algorithm;

  // Fallback options and determine whether to sort or not
  options = options || {};
  var sort = options.sort !== undefined ? options.sort : true;
  this.sort = sort;
}
PackingSmith.prototype = {
  /**
   * @param {Object} item Item to store -- this currently is mutated in-memory
   * @param {Number} item.width Width of the item
   * @param {Number} item.height Height of the item
   * @param {Mixed} [item.meta] Any meta data you would like to store related to the item
   */
  'addItem': function (item) {
    // Save the item for later
    this.items.push(item);
  },
  // Method to normalize coordinates to 0, 0
  // This is bad to do mid-addition since it messes up the algorithm
  'normalizeCoordinates': function () {
    // Grab the items
    var items = this.items;

    // Find the most negative x and y
    var minX = Infinity,
        minY = Infinity;
    items.forEach(function (item) {
      var coords = item;
      minX = Math.min(minX, coords.x);
      minY = Math.min(minY, coords.y);
    });

    // Offset each item by -minX, -minY; effectively resetting to 0, 0
    items.forEach(function (item) {
      var coords = item;
      coords.x -= minX;
      coords.y -= minY;
    });
  },
  'getStats': function () {
    // Get the endX and endY for each item
    var items = this.items,
        coordsArr = items.map(function (item) {
          return item;
        }),
        minXArr = coordsArr.map(function (coords) {
          return coords.x;
        }),
        minYArr = coordsArr.map(function (coords) {
          return coords.y;
        }),
        maxXArr = coordsArr.map(function (coords) {
          return coords.x + coords.width;
        }),
        maxYArr = coordsArr.map(function (coords) {
          return coords.y + coords.height;
        });

    // Get the maximums of these
    var retObj = {
          'minX': Math.max.apply(Math, minXArr),
          'minY': Math.max.apply(Math, minYArr),
          'maxX': Math.max.apply(Math, maxXArr),
          'maxY': Math.max.apply(Math, maxYArr)
        };

    // Return the stats
    return retObj;
  },
  'getItems': function () {
    return this.items;
  },
  'processItems': function () {
    // Run the items through our algorithm
    var items = this.items;
    if (this.sort) {
      items = this.algorithm.sort(items);
    }
    items = this.algorithm.placeItems(items);

    // Save the items for later
    this.items = items;

    // Return the items
    return items;
  },
  'exportItems': function () {
    // Process the items
    this.processItems();

    // Normalize the coordinates to 0, 0
    this.normalizeCoordinates();

    // Return the packed items
    return this.items;
  },
  /**
   * @returns {Object} retObj
   * @returns {Number} retObj.height Height of the processed layout
   * @returns {Number} retObj.width Width of the processed layout
   * @returns {Mixed[]} retObj.items Organized items
   */
  'export': function () {
    // Grab the stats, coordinates, and items
    var items = this.exportItems(),
        stats = this.getStats();

    // Generate and return our retObj
    var retObj = {
          'height': stats.maxY,
          'width': stats.maxX,
          'items': items
        };
    return retObj;
  }
};

// Export PackingSmith
module.exports = PackingSmith;
