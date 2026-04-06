module io.jooby.trpc.jackson3 {
  exports io.jooby.trpc.jackson3;

  requires io.jooby;
  requires io.jooby.trpc;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires tools.jackson.core;
  requires tools.jackson.databind;
}
