/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.email;

import javax.inject.Provider;

import org.apache.commons.mail.ImageHtmlEmail;

import com.typesafe.config.Config;

public class ImageHtmlEmailProvider implements Provider<ImageHtmlEmail> {

  private final EmailFactory factory;

  public ImageHtmlEmailProvider(final Config mail) {
    factory = new EmailFactory(mail);
  }

  @Override
  public ImageHtmlEmail get() {
    return factory.newEmail(new ImageHtmlEmail());
  }
}
