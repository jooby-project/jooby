module io.jooby.trpc.jackson2 {
  exports io.jooby.trpc.jackson2;

  requires io.jooby;
  requires io.jooby.trpc;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires com.fasterxml.jackson.databind;
}
