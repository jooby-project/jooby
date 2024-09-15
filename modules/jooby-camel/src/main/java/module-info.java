module io.jooby.camel {
  exports io.jooby.camel;

  requires io.jooby;
  requires typesafe.config;
  requires jakarta.inject;
  requires static com.github.spotbugs.annotations;
  requires camel.core.model;
  requires camel.core.engine;
  requires camel.base;
  requires camel.base.engine;
  requires camel.api;
  requires camel.support;
  requires camel.main;
}
