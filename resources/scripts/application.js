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
	
	if (window.ZeroClipboard) {
    ZeroClipboard.config( { swfPath: "http://jooby.org/resources/scripts/ZeroClipboard.swf" } );
  }
	
	$('.copy-bar').each(function () {
    var $el = $(this),
        $button = $el.find('.copy-button');

    var client = new ZeroClipboard($button);

    client.on( 'ready', function(event) {
      client.on( 'copy', function(event) {
        event.clipboardData.setData('text/plain', $el.parent().find('pre code').text());
      });
    });

  });
});