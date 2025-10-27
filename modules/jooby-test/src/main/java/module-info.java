/** Jooby test module. */
module io.jooby.test {
  exports io.jooby.test;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires org.junit.jupiter.api;
}
