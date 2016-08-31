// Add in top-down algorithm
exports.sort = function (items) {
  // Sort the items by their height
  items.sort(function (a, b) {
    return a.height - b.height;
  });
  return items;
};

exports.placeItems = function (items) {
  // Iterate over each of the items
  var y = 0;
  items.forEach(function (item) {
    // Update the y to the current height
    item.x = 0;
    item.y = y;

    // Increment the y by the item's height
    y += item.height;
  });

  // Return the items
  return items;
};
