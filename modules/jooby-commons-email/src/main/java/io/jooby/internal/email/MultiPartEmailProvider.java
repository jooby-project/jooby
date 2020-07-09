/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.email;

import javax.inject.Provider;

import org.apache.commons.mail.MultiPartEmail;

import com.typesafe.config.Config;

public class MultiPartEmailProvider implements Provider<MultiPartEmail> {

  private final EmailFactory factory;

  public MultiPartEmailProvider(final Config mail) {
    factory = new EmailFactory(mail);
  }

  @Override
  public MultiPartEmail get() {
    return factory.newEmail(new MultiPartEmail());
  }
}
