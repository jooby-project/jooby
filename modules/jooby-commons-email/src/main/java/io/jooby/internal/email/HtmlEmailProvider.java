/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.email;

import javax.inject.Provider;

import org.apache.commons.mail.HtmlEmail;

import com.typesafe.config.Config;

public class HtmlEmailProvider implements Provider<HtmlEmail> {

  private final EmailFactory factory;

  public HtmlEmailProvider(final Config mail) {
    factory = new EmailFactory(mail);
  }

  @Override
  public HtmlEmail get() {
    return factory.newEmail(new HtmlEmail());
  }
}
