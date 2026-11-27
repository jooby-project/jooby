import io.jooby.LoggingService;
import io.jooby.rainbowgum.RainbowGumBootstrap;
import io.jooby.rainbowgum.RainbowGumService;
import io.jstach.rainbowgum.spi.RainbowGumServiceProvider;

/** Rainbow Gum logging system. */
module io.jooby.rainbowgum {
  exports io.jooby.rainbowgum;

  requires io.jooby;
  requires static org.jspecify;
  requires org.slf4j;
  requires typesafe.config;
  requires io.jstach.rainbowgum;
  requires io.jstach.rainbowgum.slf4j;
  requires io.jstach.rainbowgum.pattern;
  requires io.jstach.rainbowgum.file;

  uses RainbowGumServiceProvider;

  provides LoggingService with
      RainbowGumService;
  provides RainbowGumServiceProvider with
      RainbowGumBootstrap;
}
