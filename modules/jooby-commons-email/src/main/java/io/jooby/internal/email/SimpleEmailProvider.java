/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.email;

import javax.inject.Provider;

import org.apache.commons.mail.SimpleEmail;

import com.typesafe.config.Config;

public class SimpleEmailProvider implements Provider<SimpleEmail> {

  private final EmailFactory factory;

  public SimpleEmailProvider(final Config mail) {
    factory = new EmailFactory(mail);
  }

  @Override
  public SimpleEmail get() {
    return factory.newEmail(new SimpleEmail());
  }
}
