import io.jooby.LoggingService;
import io.jooby.logback.LogbackService;

module io.jooby.logback {
  exports io.jooby.logback;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires org.slf4j;
  requires ch.qos.logback.classic;

  provides LoggingService with
      LogbackService;
}
