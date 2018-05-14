module.exports = {

   existsSync: function (path) {
     return assets.exists(path);
   },

   statSync: function (path) {
     return {
       isDirectory: function () {
         return path.indexOf('.') <= 0;
       },
       isFile: function () {
         return path.indexOf('.') > 0;
       }
     };
   },

   readFileSync: function (path, charset) {
     return assets.readFile(path);
   }
};
