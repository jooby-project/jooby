/** Reactor module */
module io.jooby.vertx {
  exports io.jooby.vertx;

  requires io.jooby;
  requires io.jooby.netty;
  requires io.vertx.core;
  requires io.netty.transport;
  requires org.slf4j;
  requires static com.github.spotbugs.annotations;
}
