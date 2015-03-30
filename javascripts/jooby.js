(function ($) {

  if (window.ZeroClipboard) {
    ZeroClipboard.config( { swfPath: "http://jooby.org/javascripts/ZeroClipboard.swf" } );
  }

  /**
   * Find the first .highlight element and prepend a toolbar with a copy to clipboard button.
   */
  var copyToClipboard = function () {
    var $button = $('<span class="copy-button octicon octicon-clippy" title="copy to clipboard"></span>');
    var $div = $('<div class="copy-bar"></div>')
    $('.highlight').prepend($div.append($button));

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
        var project = href.substring(prefix.length);
        if (project !== 'coverage-report') {
          $a.attr('href', '/doc/' + project);
        }
      }
    });
  };

  var page = function () {
    var page = window.location.pathname.replace(/\//g, '');
    $(document.body).addClass(page);
  };

  /**
   * DOM ready!
   */
  $(function () {
    // page
    page();
    // sync links
    links();
    // clipboard
    if (window.ZeroClipboard) {
      copyToClipboard();
    }
  });


})(jQuery);
