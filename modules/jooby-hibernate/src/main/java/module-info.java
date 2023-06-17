/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/**
 * Hibernate module.
 */
module io.jooby.hibernate {
  exports io.jooby.hibernate;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires org.hibernate.orm.core;
  requires java.sql;
  requires jakarta.inject;
  requires jakarta.persistence;
  requires java.naming;
}
