'use strict';
var path = require('path');

module.exports = function (str, options) {
	if (typeof str !== 'string') {
		throw new Error('Expected a string');
	}

	options = options || {};

	var pathName = str;

	if (options.resolve !== false) {
		pathName = path.resolve(str);
	}

	pathName = pathName.replace(/\\/g, '/');

	// Windows drive letter must be prefixed with a slash
	if (pathName[0] !== '/') {
		pathName = '/' + pathName;
	}

	return encodeURI('file://' + pathName);
};
