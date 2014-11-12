(function ($) {
  var version = 'unknown';

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
    });
  });

})(jQuery);
