/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
module io.jooby.openapi {
  exports io.jooby.openapi;

  requires io.jooby;
  requires com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires jakarta.inject;
  requires jakarta.ws.rs;
  requires io.swagger.v3.core;
  requires io.swagger.v3.oas.models;
  requires io.swagger.v3.oas.annotations;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires static org.apache.commons.io;
  requires org.objectweb.asm;
  requires static org.objectweb.asm.tree;
  requires static org.objectweb.asm.tree.analysis;
  requires org.objectweb.asm.util;
}
