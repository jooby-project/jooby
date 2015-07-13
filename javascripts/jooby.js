(function ($) {

  if (window.ZeroClipboard) {
    ZeroClipboard.config( { swfPath: "http://jooby.org/javascripts/ZeroClipboard.swf" } );
  }

  /**
   * Find the first .highlight element and prepend a toolbar with a copy to clipboard button.
   */
  var copyToClipboard = function () {

    $('.highlight').each(function () {
      var $button = $('<span class="copy-button octicon octicon-clippy" title="copy to clipboard"></span>');
      var $div = $('<div class="copy-bar"></div>');
      var $el = $(this);

      $el.prepend($div.append($button));

      var client = new ZeroClipboard($button);

      client.on( 'ready', function(event) {
        client.on( 'copy', function(event) {
          event.clipboardData.setData('text/plain', $el.find('pre code').text());
        });
      });

    });
  };

  var lang = function () {
    $('ul.nav-lang li').click(function () {
      var $li = $(this),
          lang = $li.data('lang')
          $ullang = $('ul.lang');
      $li.parent().find('li').removeClass('active');
      $li.addClass('active');

      $ullang.find('li').removeClass('active');
      $ullang.find('li.' + lang).addClass('active');
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
    // lang
    lang();
    // clipboard
    if (window.ZeroClipboard) {
      copyToClipboard();
    }
  });


})(jQuery);
