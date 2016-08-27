// Add in reverse-diagonal algorithm
exports.sort = function (items) {
  // Sort the items by their diagonal
  items.sort(function (a, b) {
    var aDiag = Math.sqrt(Math.pow(a.height, 2) + Math.pow(a.width, 2)),
        bDiag = Math.sqrt(Math.pow(b.height, 2) + Math.pow(b.width, 2));
    return aDiag - bDiag;
  });
  return items;
};

exports.placeItems = function (items) {
  // Iterate over each of the items
  var x = 0,
      y = 0;
  items.forEach(function (item) {
    var itemWidth = item.width;
    item.x = x - itemWidth;
    item.y = y;

    // Decrement the x and increment the y by the item's dimensions
    x -= itemWidth;
    y += item.height;
  });

  // Return the items
  return items;
};
