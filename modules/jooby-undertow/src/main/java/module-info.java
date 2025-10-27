module io.jooby.undertow {
  exports io.jooby.undertow;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires java.logging;
  // All these are jar named modules :S
  requires undertow.core;
  requires xnio.api;
  requires static org.conscrypt;
}
