# layout [![Build status](https://travis-ci.org/twolfson/layout.png?branch=master)](https://travis-ci.org/twolfson/layout)

Organize and layout items based on various algorithms

Visualizations of output data:

|         `top-down`        |          `left-right`         |         `diagonal`        |           `alt-diagonal`          |          `binary-tree`          |
|---------------------------|-------------------------------|---------------------------|-----------------------------------|---------------------------------|
| ![top-down][top-down-img] | ![left-right][left-right-img] | ![diagonal][diagonal-img] | ![alt-diagonal][alt-diagonal-img] | ![binary-tree][binary-tree-img] |

[top-down-img]: docs/top-down.png
[left-right-img]: docs/left-right.png
[diagonal-img]: docs/diagonal.png
[alt-diagonal-img]: docs/alt-diagonal.png
[binary-tree-img]: docs/binary-tree.png

## Getting Started
Install the module with: `npm install layout`

```js
// Load in layout
var layout = require('layout');

// Generate a new layer to organize items on
var layer = layout('top-down');

// Add items that you want to organize
layer.addItem({'height': 20, 'width': 10, 'meta': 'medium'});
layer.addItem({'height': 10, 'width': 10, 'meta': 'small'});
layer.addItem({'height': 50, 'width': 40, 'meta': 'large'});

// Export the info
var info = layer['export']();

// We get back the width and height of the pack as well as organized items
{
    height: 80,
    width: 40,
    items: [{
        height: 10,
        width: 10,
        meta: 'small',
        x: 0,
        y: 0
    }, {
        height: 20,
        width: 10,
        meta: 'medium',
        x: 0,
        y: 10
    }, {
        height: 50,
        width: 40,
        meta: 'large',
        x: 0,
        y: 30
    }]
}
```

## Documentation
Layout is a constructor function

```js
/**
 * Layout adds items in an algorithmic fashion
 * @constructor
 * @param {String|Object} [algorithm="top-down"] Name of algorithm or custom algorithm to use
 *   Available algorithms are listed in the Algorithms section
 * @param {Mixed} [options] Options to provide for the algorithm
 */
```

Items can be added via `addItem` which are required to have a `height` and `width`. Any additional info should be stored inside of `meta`.

```js
/**
 * @param {Object} item Item to store -- this currently is mutated in-memory
 * @param {Number} item.width Width of the item
 * @param {Number} item.height Height of the item
 * @param {Mixed} [item.meta] Any meta data you would like to store related to the item
 */
```

`export` is how you take your items and organize them.

```js
/**
 * @returns {Object} retObj
 * @returns {Number} retObj.height Height of the processed layout
 * @returns {Number} retObj.width Width of the processed layout
 * @returns {Mixed[]} retObj.items Organized items
 */
```

### Algorithms
Currently `layout` supports 5 different layout types which are listed below.

#### `top-down`
The `top-down` algorithm places items vertically.

![top-down image][top-down-img]

By default, it sorts from smallest (top) to largest (bottom). However, this can be disabled via `sort: false`.

**Options:**

- sort `Boolean` Flag to enable/disable sorting from smallest (top) to largest (bottom)
    - By default, this is enabled (`true`)

#### `left-right`
The `left-right` algorithm places items horizontally.

![left-right image][left-right-img]

By default, it sorts from smallest (left) to largest (right). However, this can be disabled via `sort: false`.

**Options:**

- sort `Boolean` Flag to enable/disable sorting from smallest (left) to largest (right)
    - By default, this is enabled (`true`)

#### `diagonal`
The `diagonal` algorithm places items diagonally (top-left to bottom-right).

![diagonal image][diagonal-img]

By default, it sorts from smallest (top-left) to largest (bottom-right). However, this can be disabled via `sort: false`.

**Options:**

- sort `Boolean` Flag to enable/disable sorting from smallest (top-left) to largest (bottom-right)
    - By default, this is enabled (`true`)

#### `alt-diagonal`
The `alt-diagonal` algorithm places items diagonally (top-right to bottom-left).

![alt-diagonal image][alt-diagonal-img]

By default, it sorts from smallest (top-right) to largest (bottom-left). However, this can be disabled via `sort: false`.

**Options:**

- sort `Boolean` Flag to enable/disable sorting from smallest (top-right) to largest (bottom-left)
    - By default, this is enabled (`true`)

#### `binary-tree`
The `binary-tree` algorithm packs items via the [binary tree algorithm][].

This is an efficient way to pack items into the smallest container possible.

[binary tree algorithm]: http://codeincomplete.com/posts/2011/5/7/bin_packing/

![binary-tree image][binary-tree-img]

### Custom algorithms
You can add your own algorithm via `layout.addAlgorithm`
```js
/**
 * Method to add new algorithms via
 * @param {String} name Name of algorithm
 * @param {Object} algorithm Algorithm to bind under name
 * @param {Function} algorithm.sort Algorithm to sort object by
 * @param {Function} algorithm.placeItems Algorithm to place items by
 */
```

## Contributing
In lieu of a formal styleguide, take care to maintain the existing coding style. Add unit tests for any new or changed functionality. Lint via `npm run lint` and test via `npm test`.

## Donating
Support this project and [others by twolfson][gratipay] via [gratipay][].

[![Support via Gratipay][gratipay-badge]][gratipay]

[gratipay-badge]: https://cdn.rawgit.com/gratipay/gratipay-badge/2.x.x/dist/gratipay.png
[gratipay]: https://www.gratipay.com/twolfson/

## License
Copyright (c) 2012-2014 Todd Wolfson
Licensed under the MIT license.
