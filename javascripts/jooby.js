(function ($) {
  var version = 'unknown';

  var copyToClipboard = function () {
    var $button = $('<button class="copy-btn">copy</button>');
    $('#dependency').append($button);
    var copy_sel = $('.copy-btn');

    // Disables other default handlers on click (avoid issues)
    copy_sel.on('click', function(e) {
        e.preventDefault();
    });

    // Apply clipboard click event
    copy_sel.clipboard({
        path: 'http://jooby.org/javascripts/jquery.clipboard.swf',

        copy: function() {
            var $el = $(this);

            // Hide "Copy" and show "Copied, copy again?" message in link
            $el.find('.code-copy-first').hide();
            $el.find('.code-copy-done').show();

            // Return text in closest element (useful when you have multiple boxes that can be copied)
            return $el.closest('.highlight pre code').text();
        }
    });
  };

  var setversion = function (version) {
    $('.version').each(function () {
      var $version = $(this);

      $version.attr('href', 'http://search.maven.org/#artifactdetails|org.jooby|jooby|' + version + '|');
      $version.html('v' + version);
    });
  };

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

  $.ajax({
    url: '/md.json',
    dataType: 'json'
  }).done(function (md) {
    version = md.version;
  }).always(function () {
    /**
     * DOM ready!
     */
    $(function () {
      setversion(version);
      // sync links
      links();
      // clipboard
      copyToClipboard();
    });
  });

})(jQuery);
