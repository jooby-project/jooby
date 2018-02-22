var APP = APP || {};

/*
  HELPER
*/
APP.isPage = function( page ){
  return $( "body" ).hasClass( "page-"+page );
}
/*
  SLIDERS
*/
APP.Sliders = {};
APP.Sliders.init = function(){
  var $sliders = $( "[data-slider]" );
  $sliders.each(function(){
    var $slider = $( this ),
      options = {
        cssWidthAndHeight: false,
        watchActiveIndex: true,
        autoplay: 3000
      };
    if( $slider.find( '.swiper-btn' ).length > 0 ){
      options.nextButton = $slider.find( '.swiper-btn-next' );
      options.prevButton = $slider.find( '.swiper-btn-prev' );
    }
    if( $slider.data( "pagination" ) != undefined ){
      options.pagination = $slider.find( $slider.data( "pagination" ) );
      options.paginationClickable = true;
    }
    if( $slider.data( "slidesperview" ) != undefined ){
      options.slidesPerView = $slider.data( "slidesperview" );
    }
    if( $slider.data( "slidespercolumn" ) != undefined ){
      options.slidesPerColumn = $slider.data( "slidespercolumn" );
    }
    if( $slider.data( "slidespergroup" ) != undefined ){
      options.slidesPerGroup = $slider.data( "slidespergroup" );
    }
    if( $slider.data( "space" ) != undefined ){
      options.spaceBetween = $slider.data( "space" );
    }
    if( $slider.data( "effect" ) != undefined ){
      options.effect = $slider.data( "effect" );
    }
    if( $slider.data( "lazyload" ) != undefined ){
      // Disable preloading of all images
      options.preloadImages = false;
      // Enable lazy loading
      options.lazyLoading = true;
      //options.lazyLoadingInPrevNext = true;
    }
    $slider.find( ".swiper-container" ).swiper( options );
    $slider.removeAttr( "data-swiper" );
  });
};
/*
  APP INITIALIZE
*/
APP.init = function(){
  //GENERATE SLIDERS
  APP.Sliders.init();

  $( ".datalist" ).on( "click", ".datalist-title", function( e ){
    e.preventDefault();
    $( this ).parent().toggleClass( "active" );
    $( this ).siblings( ".datalist-content" ).slideToggle();
  });

  $( ".site-header" ).on( "click", ".open-menu", function( e ){
    e.preventDefault();
    $( this ).toggleClass( "active" );
    $( ".site-header" ).find( ".site-nav" ).toggleClass( "active" );
  });

  $( ".nav-lang" ).on( "click", "li", function(){
    var $this = $( this );
    $this.siblings().removeClass( "active" );
    $this.addClass( "active" );
    $this.parent().siblings( ".lang" ).find( "li" ).removeClass( "active" );
    $this.parent().siblings( ".lang" ).find( "li" ).eq( $this.index() ).addClass( "active" );
  });

  var sync = function(e){
    var $other = $('.section-sidebar'), other = $other.get(0);
    var percentage = this.scrollTop / (this.scrollHeight - this.offsetHeight);
    other.scrollTop = percentage * (other.scrollHeight - other.offsetHeight);
    setTimeout( function(){ $other.on('scroll', sync ); },10);
  }

  if( APP.isPage( "sidebar" ) ){
    var $sectionSidebar = $( ".section-sidebar" );
    $sectionSidebar.on( "click", "a", function( e ){
      e.preventDefault();
      var hash = $( this ).attr( "href" );
      $sectionSidebar.find( "li" ).removeClass( "active" );
      $( this ).parent().addClass( "active" );
      if( $( hash ).length > 0 ){
        $( "html, body" ).animate({
          scrollTop: $( hash ).offset().top - 140
        }, 1000, function(){
          window.top.location.hash = hash;
        });
      }  
    });
    $sectionSidebar.on( "click", ".menu-open", function( e ){
      e.preventDefault();
      $sectionSidebar.toggleClass( "active" );
    });
    $sectionSidebar.on( "click", "a", function( e ){
      $sectionSidebar.find( ".menu-open" ).html( $( this ).text() );
      $sectionSidebar.removeClass( "active" );
    });
    $( window ).on( "resize scroll", function(){
      var left = $sectionSidebar.parent().offset().left,
        top = $( this ).scrollTop(),
        stop = $( ".site-footer" ).offset().top - $( this ).height();
      bottom = 20;
      if( top > stop ){
        bottom = 20 + ( top - stop );
      }
      $sectionSidebar.css({
        position: "fixed",
        left: left,
        top: 160,
        bottom: bottom
      });
    });
    $( window ).trigger( "resize" );
  }
};

$(function(){
  APP.init();
  
  $('.jsback').click(function (event) {
    event.preventDefault();
    window.history.back();
  });

  $('.copy-bar').each(function () {
    var $el = $(this),
        $button = $el.find('.copy-button');

    $button.click(function () {
      var textArea = document.createElement("textarea");

      //
      // *** This styling is an extra step which is likely not required. ***
      //
      // Why is it here? To ensure:
      // 1. the element is able to have focus and selection.
      // 2. if element was to flash render it has minimal visual impact.
      // 3. less flakyness with selection and copying which **might** occur if
      //    the textarea element is not visible.
      //
      // The likelihood is the element won't even render, not even a flash,
      // so some of these are just precautions. However in IE the element
      // is visible whilst the popup box asking the user for permission for
      // the web page to copy to the clipboard.
      //

      // Place in top-left corner of screen regardless of scroll position.
      textArea.style.position = 'fixed';
      textArea.style.top = 0;
      textArea.style.left = 0;

      // Ensure it has a small width and height. Setting to 1px / 1em
      // doesn't work as this gives a negative w/h on some browsers.
      textArea.style.width = '2em';
      textArea.style.height = '2em';

      // We don't need padding, reducing the size if it does flash render.
      textArea.style.padding = 0;

      // Clean up any borders.
      textArea.style.border = 'none';
      textArea.style.outline = 'none';
      textArea.style.boxShadow = 'none';

      // Avoid flash of white box if rendered for any reason.
      textArea.style.background = 'transparent';

      textArea.value = $el.parent().find('pre code').text().trim();

      document.body.appendChild(textArea);

      textArea.select();

      try {
        var successful = document.execCommand('copy');
        var msg = successful ? 'successful' : 'unsuccessful';
        console.log('Copying text command was ' + msg);
      } catch (err) {
        console.log('Oops, unable to copy');
      }

      document.body.removeChild(textArea);
    });
  });

  $('span.nt:contains("<dependency>")').each(function () {
    var $el = $(this),
        $dep = $el.parent().parent().parent(),
        $maven = $($dep.text()),
        groupId = $maven.find('groupId').text(),
        artifactId = $maven.find('artifactId').text(),
        version = $maven.find('version').text(),
        scope = $maven.find('scope').text() === 'provided' ? 'compileOnly' : 'compile',
        $gradle = $('<div class="highlighter-rouge codehilite">' +
            '<div class="copy-bar">' +
             '<span class="icon-clipboard-big copy-button octicon octicon-clippy" title="copy to clipboard"></span>' +
            '</div>' +
            '<pre class="highlight"><code><span class="nt">' + scope + '</span><span class="p">:</span> \'' + groupId + ':' + artifactId + ':' + version + '\'</code></pre>' +
           '</div>');

    $gradle.hide();
    $gradle.height($dep.height());

    $dep.before('<p style="margin: 10px 0 0 0;"><span class="build active">maven</span> | <span class="build">gradle</span></p>');
    $dep.after($gradle);
    $dep.addClass('maven build');
    $gradle.addClass('gradle build');
  });

  var $builds = $('div.build');
  $('span.build').click(function () {
    var $el = $(this),
        build = $el.text();

    $('span.build').removeClass('active');
    $el.addClass('active');
    $builds.hide();
    $('div.' + build).show();
  });
});