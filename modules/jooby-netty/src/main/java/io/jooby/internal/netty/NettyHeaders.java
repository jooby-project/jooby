/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;

public class NettyHeaders extends DefaultHttpHeaders {
  static final DefaultHeaders.NameValidator<CharSequence> NAME_VALIDATOR =
      DefaultHttpHeadersFactory.headersFactory().withNameValidation(false).getNameValidator();
  static final DefaultHeaders.ValueValidator<CharSequence> VALUE_VALIDATOR =
      DefaultHttpHeadersFactory.headersFactory().withValidation(false).getValueValidator();

  public NettyHeaders() {
    super(NAME_VALIDATOR, VALUE_VALIDATOR);
  }
}
