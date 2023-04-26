import io.jooby.ResultHandler;
import io.jooby.reactor.Reactor;

module io.jooby.reactor {
  exports io.jooby.reactor;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires reactor.core;
  requires org.reactivestreams;
  requires org.slf4j;

  provides ResultHandler with
      Reactor;
}
