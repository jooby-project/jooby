/** Open API module. */
module io.jooby.openapi {
  exports io.jooby.openapi;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires io.swagger.v3.core;
  requires io.swagger.v3.oas.models;
  requires io.swagger.v3.oas.annotations;
  requires com.puppycrawl.tools.checkstyle;
  requires jakarta.inject;
  requires jakarta.ws.rs;
  requires org.objectweb.asm;
  requires org.objectweb.asm.tree;
  requires org.objectweb.asm.util;
}
