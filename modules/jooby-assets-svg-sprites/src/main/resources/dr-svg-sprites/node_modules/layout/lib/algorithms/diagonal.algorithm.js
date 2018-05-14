// Add in diagonal algorithm
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
    // Update the x to the current width
    item.x = x;
    item.y = y;

    // Increment the x and y by the item's dimensions
    x += item.width;
    y += item.height;
  });

  // Return the items
  return items;
};
