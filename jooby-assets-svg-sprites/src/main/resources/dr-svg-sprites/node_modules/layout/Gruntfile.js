// Define our grunt file
module.exports = function (grunt) {
  // Configure the spritesheets
  grunt.initConfig({
    sprite: {
      'top-down': {
        src: ['docs/sprite.*.png'],
        destImg: 'docs/top-down.png',
        destCSS: 'docs/top-down.css',
        algorithm: 'top-down'
      },
      'left-right': {
        src: ['docs/sprite.*.png'],
        destImg: 'docs/left-right.png',
        destCSS: 'docs/left-right.css',
        algorithm: 'left-right'
      },
      'diagonal': {
        src: ['docs/sprite.*.png'],
        destImg: 'docs/diagonal.png',
        destCSS: 'docs/diagonal.css',
        algorithm: 'diagonal'
      },
      'alt-diagonal': {
        src: ['docs/sprite.*.png'],
        destImg: 'docs/alt-diagonal.png',
        destCSS: 'docs/alt-diagonal.css',
        algorithm: 'alt-diagonal'
      },
      'binary-tree': {
        src: ['docs/sprite.*.png'],
        destImg: 'docs/binary-tree.png',
        destCSS: 'docs/binary-tree.css',
        algorithm: 'binary-tree'
      },
    }
  });

  // Load in grunt-spritesmith
  grunt.loadNpmTasks('grunt-spritesmith');
};
