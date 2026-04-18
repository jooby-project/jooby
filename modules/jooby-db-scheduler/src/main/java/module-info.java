module io.jooby.dbscheduler {
  exports io.jooby.dbscheduler;

  requires io.jooby;
  requires static org.jspecify;
  requires typesafe.config;
  requires java.sql;
  requires com.github.kagkarlsson.scheduler;
  requires org.slf4j;
}
