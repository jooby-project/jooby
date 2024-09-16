import io.jooby.LoggingService;
import io.jooby.log4j.Log4jService;

module io.jooby.log4j {
  exports io.jooby.log4j;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires org.slf4j;
  requires org.apache.logging.log4j;
  requires org.apache.logging.log4j.core;
  requires org.jspecify;

  provides LoggingService with
      Log4jService;
}
