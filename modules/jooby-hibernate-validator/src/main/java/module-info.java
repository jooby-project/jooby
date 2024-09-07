/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/** Hibernate Validator Module. */
module io.jooby.hibernate.validator {
  exports io.jooby.hibernate.validator;

  requires transitive io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.hibernate.validator;
  requires jakarta.validation;
}
