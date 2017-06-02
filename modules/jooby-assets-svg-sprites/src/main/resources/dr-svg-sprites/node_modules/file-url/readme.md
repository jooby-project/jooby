# file-url [![Build Status](https://travis-ci.org/sindresorhus/file-url.svg?branch=master)](https://travis-ci.org/sindresorhus/file-url)

> Convert a path to a file url: `unicorn.jpg` → `file:///Users/sindresorhus/unicorn.jpg`


## Install

```
$ npm install --save file-url
```


## Usage

```js
const fileUrl = require('file-url');

fileUrl('unicorn.jpg');
//=> 'file:///Users/sindresorhus/dev/file-url/unicorn.jpg'

fileUrl('/Users/pony/pics/unicorn.jpg');
//=> 'file:///Users/pony/pics/unicorn.jpg'

// passing {resolve: false} will make it not call path.resolve() on the path
fileUrl('unicorn.jpg', {resolve: false});
//=> 'file:///unicorn.jpg'
```


## CLI

```
$ npm install --global file-url
```

```
$ file-url --help

  Usage
    $ file-url [path]

  Example
    $ file-url
    file:///Users/sindresorhus/dev/file-url
```


## License

MIT © [Sindre Sorhus](http://sindresorhus.com)
