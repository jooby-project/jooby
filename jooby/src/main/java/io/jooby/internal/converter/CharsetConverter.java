/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.ValueNode;
import io.jooby.ValueConverter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CharsetConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == Charset.class;
  }

  @Override public Charset convert(ValueNode value, Class type) {
    String charset = value.value();
    switch (charset.toLowerCase()) {
      case "utf-8":
        return StandardCharsets.UTF_8;
      case "us-ascii":
        return StandardCharsets.US_ASCII;
      case "iso-8859-1":
        return StandardCharsets.ISO_8859_1;
      case "utf-16":
        return StandardCharsets.UTF_16;
      case "utf-16be":
        return StandardCharsets.UTF_16BE;
      case "utf-16le":
        return StandardCharsets.UTF_16LE;
      default:
        return Charset.forName(charset);
    }
  }
}
