var fs = require("fs");
var path = require("path");
var handlebars = require("handlebars");
var util = require("./util");

module.exports = function (sprite, callback) {

	var config = sprite.config;

	handlebars.registerHelper("url", function (filepath, relation) {
		if (typeof relation == "string") {
			relation = path.dirname(relation);
		}
		else {
			relation = config.cssPath;
		}
		return path.relative(relation, filepath).replace(/\\/g, "/");
	});
	
	handlebars.registerHelper("attValue", function (name) {
		return name && name.replace(/^\./g, "");
	});

	var templatePath = path.join(__dirname, "../templates/preview.hbs");

	fs.readFile(templatePath, "utf-8", function (err, template) {
		if (err) {
			throw err;
		}

		var compiler = handlebars.compile(template);
		var source = compiler(sprite);

		util.write(sprite.previewPath, source, callback);
	});

};