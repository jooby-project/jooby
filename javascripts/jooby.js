(function ($) {

  ZeroClipboard.config( { swfPath: "http://jooby.org/javascripts/ZeroClipboard.swf" } );

  /**
   * Find the first .highlight element and prepend a toolbar with a copy to clipboard button.
   */
  var copyToClipboard = function () {
    var $button = $('<span class="copy-button" title="copy to clipboard">copy</span>');
    var $div = $('<div class="copy-bar"></div>')
    $('.highlight').first().prepend($div.append($button));

    var client = new ZeroClipboard($button);

    client.on( 'ready', function(event) {

      client.on( 'copy', function(event) {
        event.clipboardData.setData('text/plain', $button.parent().parent().find('pre code').text());
      });

    });
  };


  /** Find links and rewrite them to local version of doc (not github path). */
  var links = function () {
    $('a').each(function () {
      var $a = $(this),
          href = $a.attr('href'),
          prefix = 'https://github.com/jooby-project/jooby/tree/master/',
          idx = href.indexOf(prefix);

      if (idx === 0) {
        $a.attr('href', '/doc/' + href.substring(prefix.length));
      }
    });
  };


  /**
   * DOM ready!
   */
  $(function () {
    // sync links
    links();
    // clipboard
    copyToClipboard();
  });


})(jQuery);
