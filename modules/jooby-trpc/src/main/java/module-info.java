module io.jooby.trpc {
  exports io.jooby.trpc;
  exports io.jooby.annotation.trpc;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
}
