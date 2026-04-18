import io.jooby.LoggingService;
import io.jooby.log4j.Log4jService;

/** Log4j logging system. */
module io.jooby.log4j {
  exports io.jooby.log4j;

  requires io.jooby;
  requires static org.jspecify;
  requires org.slf4j;
  requires org.apache.logging.log4j;
  requires org.apache.logging.log4j.core;

  provides LoggingService with
      Log4jService;
}
